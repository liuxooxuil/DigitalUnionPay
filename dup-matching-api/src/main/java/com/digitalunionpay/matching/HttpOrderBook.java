package com.digitalunionpay.matching;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpOrderBook implements OrderBook {
  private final RestClient client;

  public HttpOrderBook(RestClient client) {
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
  public void accept(String orderId, String pair, String side, Money price, long quantityMinor) {
    client.post()
        .uri(MatchingPaths.ORDERS)
        .body(new AcceptOrder(orderId, pair, side, price, quantityMinor))
        .retrieve()
        .toBodilessEntity();
  }
}
