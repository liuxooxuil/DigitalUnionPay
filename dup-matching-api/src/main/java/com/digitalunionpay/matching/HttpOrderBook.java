package com.digitalunionpay.matching;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用撮合服务 8088 接单。 */
public final class HttpOrderBook implements OrderBook {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpOrderBook(RestClient client) {
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

  /** 调用撮合服务的 /internal/matching/orders，不读取响应体。 */
  @Override
  public void accept(String orderId, String pair, String side, Money price, long quantityMinor) {
    client.post()
        .uri(MatchingPaths.ORDERS)
        .body(new AcceptOrder(orderId, pair, side, price, quantityMinor))
        .retrieve()
        .toBodilessEntity();
  }
}
