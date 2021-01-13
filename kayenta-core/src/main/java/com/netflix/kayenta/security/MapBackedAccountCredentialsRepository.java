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

import com.netflix.spinnaker.credentials.CompositeCredentialsRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MapBackedAccountCredentialsRepository implements AccountCredentialsRepository {

  private final Map<String, AccountCredentials> accountNameToCredentialsMap;
  private final CompositeCredentialsRepository<AccountCredentials> compositeRepository;

  public MapBackedAccountCredentialsRepository() {
    this.accountNameToCredentialsMap = new ConcurrentHashMap<>();
    this.compositeRepository = new CompositeCredentialsRepository<>(Collections.emptyList());
  }

  public MapBackedAccountCredentialsRepository(
      CompositeCredentialsRepository<AccountCredentials> compositeRepository) {
    this.accountNameToCredentialsMap = new ConcurrentHashMap<>();
    this.compositeRepository = compositeRepository;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends AccountCredentials> Optional<T> getOne(String accountName) {
    AccountCredentials accountCredentials = accountNameToCredentialsMap.get(accountName);
    if (accountCredentials == null) {
      accountCredentials = compositeRepository.getFirstCredentialsWithName(accountName);
    }
    return Optional.ofNullable((T) accountCredentials);
  }

  @Override
  public Set<AccountCredentials> getAllOf(AccountCredentials.Type credentialsType) {
    Set<AccountCredentials> set =
        getAll().stream()
            .filter(credentials -> credentials.getSupportedTypes().contains(credentialsType))
            .collect(Collectors.toSet());
    return set;
  }

  @Override
  public Optional<AccountCredentials> getOne(AccountCredentials.Type credentialsType) {
    Optional<AccountCredentials> accountCredentials =
        accountNameToCredentialsMap.values().stream()
            .filter(a -> a.getSupportedTypes().contains(credentialsType))
            .findFirst();
    if (accountCredentials.isPresent()) {
      return accountCredentials;
    }
    return compositeRepository.getAllCredentials().stream()
        .filter(a -> a.getSupportedTypes().contains(credentialsType))
        .findFirst();
  }

  @Override
  public Set<AccountCredentials> getAll() {
    Set<AccountCredentials> all = new HashSet<>(accountNameToCredentialsMap.values());
    all.addAll(compositeRepository.getAllCredentials());
    return all;
  }

  @Override
  @Deprecated
  public AccountCredentials save(String name, AccountCredentials credentials) {
    return accountNameToCredentialsMap.put(credentials.getName(), credentials);
  }
}
