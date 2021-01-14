/*
 * Copyright 2020 Playtika.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static com.netflix.kayenta.security.AccountCredentials.Type.CONFIGURATION_STORE;
import static com.netflix.kayenta.security.AccountCredentials.Type.METRICS_STORE;
import static com.netflix.kayenta.security.AccountCredentials.Type.OBJECT_STORE;
import static com.netflix.kayenta.security.AccountCredentials.Type.REMOTE_JUDGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.netflix.spinnaker.credentials.CompositeCredentialsRepository;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class MapBackedAccountCredentialsRepositoryTest {
  private static final String ACCOUNT_4 = "account4";
  private static final String PROVIDER_TYPE = "providerType";

  AccountCredentialsRepository repository;

  @Mock AccountCredentials account4;

  @Before
  public void setUp() {
    when(account4.getSupportedTypes()).thenReturn(Arrays.asList(CONFIGURATION_STORE));
    CompositeCredentialsRepository compositeRepo = mock(CompositeCredentialsRepository.class);
    when(compositeRepo.getAllCredentials()).thenReturn(ImmutableList.of(account4));
    when(compositeRepo.getFirstCredentialsWithName(ACCOUNT_4)).thenReturn(account4);
    when(compositeRepo.getFirstCredentialsWithName(AdditionalMatchers.not(eq(ACCOUNT_4))))
        .thenReturn(null);
    when(compositeRepo.getCredentials(ACCOUNT_4, PROVIDER_TYPE)).thenReturn(account4);
    repository = new MapBackedAccountCredentialsRepository(compositeRepo);
  }

  @Test
  public void getOne_returnsEmptyIfAccountNotPresent() {
    assertThat(repository.getOne("account")).isEmpty();
  }

  @Test
  public void getOne_returnsPresentAccount() {
    AccountCredentials account = namedAccount("account1");
    repository.save("account1", account);

    assertThat(repository.getOne("account1")).hasValue(account);
    assertThat(repository.getOne(ACCOUNT_4)).hasValue(account4);
  }

  @Test
  public void getRequiredOne_throwsExceptionIfAccountNotPresent() {
    assertThatThrownBy(() -> repository.getRequiredOne("account"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unable to resolve account account.");
  }

  @Test
  public void getRequiredOne_returnsPresentAccount() {
    AccountCredentials account = namedAccount("account1");
    repository.save("account1", account);

    AccountCredentials actual = repository.getRequiredOne("account1");
    assertThat(actual).isEqualTo(account);
    actual = repository.getRequiredOne(ACCOUNT_4);
    assertThat(actual).isEqualTo(account4);
  }

  @Test
  public void getAllAccountsOfType_returnsAccountsOfSpecificTypeOnly() {
    AccountCredentials account1 = namedAccount("account1", METRICS_STORE, OBJECT_STORE);
    AccountCredentials account2 =
        namedAccount("account2", METRICS_STORE, OBJECT_STORE, CONFIGURATION_STORE, REMOTE_JUDGE);
    AccountCredentials account3 = namedAccount("account3");

    repository.save("account1", account1);
    repository.save("account2", account2);
    repository.save("account3", account3);

    assertThat(repository.getAllOf(METRICS_STORE)).containsOnly(account1, account2);
    assertThat(repository.getAllOf(OBJECT_STORE)).containsOnly(account1, account2);
    assertThat(repository.getAllOf(CONFIGURATION_STORE)).containsOnly(account2, account4);
    assertThat(repository.getAllOf(REMOTE_JUDGE)).containsOnly(account2);
  }

  @Test
  public void getRequiredOneBy_returnsActualAccountByName() {
    AccountCredentials account1 = namedAccount("account1", METRICS_STORE);
    repository.save("account1", account1);

    assertThat(repository.getRequiredOneBy("account1", METRICS_STORE)).isEqualTo(account1);
    assertThat(repository.getRequiredOneBy(ACCOUNT_4, CONFIGURATION_STORE)).isEqualTo(account4);
  }

  @Test
  public void getRequiredOneBy_returnsFirstAvailableAccountByTypeIfNameIsNotProvided() {
    AccountCredentials account1 = namedAccount("account1", METRICS_STORE, OBJECT_STORE);
    AccountCredentials account2 = namedAccount("account2", METRICS_STORE, OBJECT_STORE);
    AccountCredentials account3 = namedAccount("account3", METRICS_STORE, OBJECT_STORE);
    repository.save("account1", account1);
    repository.save("account2", account2);
    repository.save("account3", account3);

    assertThat(repository.getRequiredOneBy(null, METRICS_STORE)).isIn(account1, account2, account3);
    assertThat(repository.getRequiredOneBy("", METRICS_STORE)).isIn(account1, account2, account3);
    assertThat(repository.getRequiredOneBy(null, CONFIGURATION_STORE)).isIn(account4);
    assertThat(repository.getRequiredOneBy("", CONFIGURATION_STORE)).isIn(account4);
  }

  @Test
  public void getRequiredOneBy_throwsExceptionIfCannotResolveAccount() {
    assertThatThrownBy(() -> repository.getRequiredOneBy(null, METRICS_STORE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void getRequiredOneBy_nameAndType() {
    assertThat(repository.getRequiredOne(ACCOUNT_4, PROVIDER_TYPE)).isEqualTo(account4);
  }

  private AccountCredentials namedAccount(String name, AccountCredentials.Type... types) {
    AccountCredentials account = mock(AccountCredentials.class);
    when(account.getName()).thenReturn(name);
    when(account.getSupportedTypes()).thenReturn(Arrays.asList(types));
    return account;
  }
}
