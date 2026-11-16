package com.digitalunionpay.exchange;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用兑换服务 8087 锁价。 */
public final class HttpExchangePort implements ExchangePort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpExchangePort(RestClient client) {
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

  /** 调用兑换服务的 /internal/exchange/locks。 */
  @Override
  public PriceLock lock(String userId, Money source, String targetCurrency) {
    return body(
        client.post().uri(ExchangePaths.LOCKS).body(new LockRequest(userId, source, targetCurrency)).retrieve().body(PriceLock.class),
        "priceLock");
  }
}
