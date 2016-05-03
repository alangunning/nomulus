// Copyright 2016 The Domain Registry Authors. All Rights Reserved.
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

package google.registry.module.frontend;

import dagger.Component;

import google.registry.braintree.BraintreeModule;
import google.registry.config.ConfigModule;
import google.registry.keyring.api.KeyModule;
import google.registry.keyring.api.VoidKeyringModule;
import google.registry.request.Modules.UserServiceModule;
import google.registry.request.RequestModule;
import google.registry.ui.ConsoleConfigModule;
import google.registry.util.SystemClock.SystemClockModule;

import javax.inject.Singleton;

/** Dagger component with instance lifetime for "default" App Engine module. */
@Singleton
@Component(
    modules = {
        BraintreeModule.class,
        ConfigModule.class,
        ConsoleConfigModule.class,
        KeyModule.class,
        SystemClockModule.class,
        UserServiceModule.class,
        VoidKeyringModule.class,
    })
interface FrontendComponent {
  FrontendRequestComponent startRequest(RequestModule requestModule);
}