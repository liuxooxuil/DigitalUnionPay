package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpFundingCoordinator implements FundingCoordinator {
  private final RestClient client;

  public HttpFundingCoordinator(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  @Override
  public void onFiatSettled(String userId, Money amount, String providerReference) {
    client.post()
        .uri(FundingPaths.FIAT_SETTLEMENTS)
        .body(new FiatSettled(userId, amount, providerReference))
        .retrieve()
        .toBodilessEntity();
  }
}
