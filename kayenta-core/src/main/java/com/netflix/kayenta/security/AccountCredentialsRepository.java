/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.security;

import com.netflix.spinnaker.credentials.CredentialsRepository;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public interface AccountCredentialsRepository extends CredentialsRepository<AccountCredentials> {

  <T extends AccountCredentials> Optional<T> getOptionalOne(String accountName);

  <T extends AccountCredentials> T getRequiredOne(String accountName);

  Optional<AccountCredentials> getOne(AccountCredentials.Type credentialsType);

  AccountCredentials save(String name, AccountCredentials credentials);

  default AccountCredentials getRequiredOneBy(
      String accountName, AccountCredentials.Type accountType) {
    if (StringUtils.hasLength(accountName)) {
      return getRequiredOne(accountName);
    } else {
      return getOne(accountType)
          .orElseThrow(
              () ->
                  new IllegalArgumentException(
                      "Unable to resolve account of type " + accountType + "."));
    }
  }

  default Set<AccountCredentials> getAllOf(AccountCredentials.Type credentialsType) {
    return getAll().stream()
        .filter(credentials -> credentials.getSupportedTypes().contains(credentialsType))
        .collect(Collectors.toSet());
  }
}
