package com.digitalunionpay.valuation;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用估值服务 8084 的 ValuationPort。 */
public final class HttpValuationPort implements ValuationPort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpValuationPort(RestClient client) {
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

  /** 调用 POST /internal/valuation/conversions 并取回报价。 */
  @Override
  public Money convert(Money source, String targetCurrency) {
    return body(
        client.post()
            .uri(ValuationPaths.CONVERSIONS)
            .body(new ConversionRequest(source, targetCurrency))
            .retrieve()
            .body(Money.class),
        "money");
  }
}
