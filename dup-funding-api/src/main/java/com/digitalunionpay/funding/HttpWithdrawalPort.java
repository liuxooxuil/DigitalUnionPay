package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用出入金服务 8086 提交出金。 */
public final class HttpWithdrawalPort implements WithdrawalPort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpWithdrawalPort(RestClient client) {
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

  /** 调用出入金服务的 /internal/funding/withdrawals。 */
  @Override
  public String submit(String userId, Money amount, String beneficiaryRef) {
    WithdrawalId id = body(
        client.post()
            .uri(FundingPaths.WITHDRAWALS)
            .body(new SubmitWithdrawal(userId, amount, beneficiaryRef))
            .retrieve()
            .body(WithdrawalId.class),
        "withdrawalId");
    return id.value();
  }
}
