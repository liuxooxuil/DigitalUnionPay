package com.digitalunionpay.valuation;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpValuationPort implements ValuationPort {
  private final RestClient client;

  public HttpValuationPort(RestClient client) {
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
