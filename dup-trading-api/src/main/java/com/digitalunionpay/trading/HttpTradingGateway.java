package com.digitalunionpay.trading;

import org.springframework.web.client.RestClient;

/** 调用 trading-service:8091 开市价单。 */
public final class HttpTradingGateway implements TradingGateway {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpTradingGateway(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  /** 响应体为空时失败，避免把空结果当成成功。 */
  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  /** POST /internal/trading/market-orders，提交市价单。 */
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
