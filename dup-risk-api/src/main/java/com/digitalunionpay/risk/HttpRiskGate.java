package com.digitalunionpay.risk;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpRiskGate implements RiskGate {
  private final RestClient client;

  public HttpRiskGate(RestClient client) {
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
  public RiskDecision check(String userId, RiskAction action, Money amount) {
    return body(
        client.post().uri(RiskPaths.CHECKS).body(new RiskCheck(userId, action, amount)).retrieve().body(RiskDecision.class),
        "riskDecision");
  }
}
