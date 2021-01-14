/*
 * Copyright 2021 Netflix, Inc.
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
 *
 */

package com.netflix.kayenta.prometheus.config;

import com.netflix.kayenta.prometheus.security.PrometheusCredentials;
import com.netflix.kayenta.prometheus.security.PrometheusNamedAccountCredentials;
import com.netflix.kayenta.prometheus.service.PrometheusRemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.spinnaker.credentials.definition.CredentialsParser;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

@Slf4j
public class PrometheusParser
    implements CredentialsParser<PrometheusManagedAccount, PrometheusNamedAccountCredentials> {

  private PrometheusResponseConverter prometheusConverter;
  private RetrofitClientFactory retrofitClientFactory;
  private OkHttpClient okHttpClient;

  PrometheusParser(
      PrometheusResponseConverter prometheusConverter,
      RetrofitClientFactory retrofitClientFactory,
      OkHttpClient okHttpClient) {
    this.prometheusConverter = prometheusConverter;
    this.retrofitClientFactory = retrofitClientFactory;
    this.okHttpClient = okHttpClient;
  }

  @Nullable
  @Override
  public PrometheusNamedAccountCredentials parse(PrometheusManagedAccount credentials) {
    PrometheusCredentials prometheusCredentials =
        PrometheusCredentials.builder()
            .username(credentials.getUsername())
            .password(credentials.getPassword())
            .usernamePasswordFile(credentials.getUsernamePasswordFile())
            .build();
    PrometheusNamedAccountCredentials.PrometheusNamedAccountCredentialsBuilder
        prometheusNamedAccountCredentialsBuilder =
            PrometheusNamedAccountCredentials.builder()
                .name(credentials.getName())
                .endpoint(credentials.getEndpoint())
                .credentials(prometheusCredentials);

    if (!CollectionUtils.isEmpty(credentials.getSupportedTypes())) {
      if (credentials.getSupportedTypes().contains(AccountCredentials.Type.METRICS_STORE)) {
        PrometheusRemoteService prometheusRemoteService =
            retrofitClientFactory.createClient(
                PrometheusRemoteService.class,
                prometheusConverter,
                credentials.getEndpoint(),
                okHttpClient,
                credentials.getUsername(),
                credentials.getPassword(),
                credentials.getUsernamePasswordFile());

        prometheusNamedAccountCredentialsBuilder.prometheusRemoteService(prometheusRemoteService);
      }
      prometheusNamedAccountCredentialsBuilder.supportedTypes(credentials.getSupportedTypes());
    }
    PrometheusNamedAccountCredentials prometheusNamedAccountCredentials =
        prometheusNamedAccountCredentialsBuilder.build();
    return prometheusNamedAccountCredentials;
  }
}
