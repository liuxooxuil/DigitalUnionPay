package com.digitalunionpay.exchange;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpExchangePort implements ExchangePort {
  private final RestClient client;

  public HttpExchangePort(RestClient client) {
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
  public PriceLock lock(String userId, Money source, String targetCurrency) {
    return body(
        client.post().uri(ExchangePaths.LOCKS).body(new LockRequest(userId, source, targetCurrency)).retrieve().body(PriceLock.class),
        "priceLock");
  }
}
