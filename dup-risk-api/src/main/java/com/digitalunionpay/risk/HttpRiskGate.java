package com.digitalunionpay.risk;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用风控服务 8083 的 RiskGate。 */
public final class HttpRiskGate implements RiskGate {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpRiskGate(RestClient client) {
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

  /** 调用 POST /internal/risk/checks 并取回风控结论。 */
  @Override
  public RiskDecision check(String userId, RiskAction action, Money amount) {
    return body(
        client.post().uri(RiskPaths.CHECKS).body(new RiskCheck(userId, action, amount)).retrieve().body(RiskDecision.class),
        "riskDecision");
  }
}
