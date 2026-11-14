package com.digitalunionpay.trading;

import org.springframework.web.client.RestClient;

public final class HttpTradingGateway implements TradingGateway {
  private final RestClient client;

  public HttpTradingGateway(RestClient client) {
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
  public String openMarket(String userId, String symbol, String side, long lots) {
    OrderTicket ticket = body(
        client.post()
            .uri(TradingPaths.MARKET_ORDERS)
            .body(new OpenMarketOrder(userId, symbol, side, lots))
            .retrieve()
            .body(OrderTicket.class),
        "orderTicket");
    return ticket.value();
  }
}
