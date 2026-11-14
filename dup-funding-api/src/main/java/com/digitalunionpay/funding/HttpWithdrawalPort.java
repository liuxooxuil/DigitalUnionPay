package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpWithdrawalPort implements WithdrawalPort {
  private final RestClient client;

  public HttpWithdrawalPort(RestClient client) {
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
