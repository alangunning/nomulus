// Copyright 2016 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.flows.domain;

import static google.registry.flows.ResourceFlowUtils.loadAndVerifyExistence;
import static google.registry.flows.ResourceFlowUtils.verifyOptionalAuthInfoForResource;
import static google.registry.flows.domain.DomainFlowUtils.createTransferResponse;
import static google.registry.model.domain.DomainResource.extendRegistrationWithCap;
import static google.registry.model.eppoutput.Result.Code.SUCCESS;

import com.google.common.base.Optional;
import google.registry.flows.EppException;
import google.registry.flows.FlowModule.ClientId;
import google.registry.flows.FlowModule.TargetId;
import google.registry.flows.LoggedInFlow;
import google.registry.flows.exceptions.NoTransferHistoryToQueryException;
import google.registry.flows.exceptions.NotAuthorizedToViewTransferException;
import google.registry.model.domain.DomainResource;
import google.registry.model.eppcommon.AuthInfo;
import google.registry.model.eppoutput.EppOutput;
import google.registry.model.transfer.TransferData;
import google.registry.model.transfer.TransferStatus;
import javax.inject.Inject;
import org.joda.time.DateTime;

/**
 * An EPP flow that queries a pending transfer on a domain.
 *
 * <p>The "gaining" registrar requests a transfer from the "losing" (aka current) registrar. The
 * losing registrar has a "transfer" time period to respond (by default five days) after which the
 * transfer is automatically approved. This flow can be used by the gaining or losing registrars
 * (or anyone with the correct authId) to see the status of a transfer, which may still be pending
 * or may have been approved, rejected, cancelled or implicitly approved by virtue of the transfer
 * period expiring.
 *
 * @error {@link google.registry.flows.ResourceFlowUtils.BadAuthInfoForResourceException}
 * @error {@link google.registry.flows.ResourceFlowUtils.ResourceDoesNotExistException}
 * @error {@link google.registry.flows.exceptions.NoTransferHistoryToQueryException}
 * @error {@link google.registry.flows.exceptions.NotAuthorizedToViewTransferException}
 */
public final class DomainTransferQueryFlow extends LoggedInFlow {

  @Inject Optional<AuthInfo> authInfo;
  @Inject @ClientId String clientId;
  @Inject @TargetId String targetId;
  @Inject DomainTransferQueryFlow() {}

  @Override
  public final EppOutput run() throws EppException {
    DomainResource domain = loadAndVerifyExistence(DomainResource.class, targetId, now);
    verifyOptionalAuthInfoForResource(authInfo, domain);
    // Most of the fields on the transfer response are required, so there's no way to return valid
    // XML if the object has never been transferred (and hence the fields aren't populated).
    TransferData transferData = domain.getTransferData();
    if (transferData.getTransferStatus() == null) {
      throw new NoTransferHistoryToQueryException();
    }
    // Note that the authorization info on the command (if present) has already been verified. If
    // it's present, then the other checks are unnecessary.
    if (!authInfo.isPresent()
        && !clientId.equals(transferData.getGainingClientId())
        && !clientId.equals(transferData.getLosingClientId())) {
      throw new NotAuthorizedToViewTransferException();
    }
    DateTime newExpirationTime = null;
    if (transferData.getTransferStatus().isApproved()
        || transferData.getTransferStatus().equals(TransferStatus.PENDING)) {
      // TODO(b/25084229): This is not quite right.
      newExpirationTime = extendRegistrationWithCap(
          now,
          domain.getRegistrationExpirationTime(),
          transferData.getExtendedRegistrationYears());
    }
    return createOutput(SUCCESS, createTransferResponse(targetId, transferData, newExpirationTime));
  }
}
