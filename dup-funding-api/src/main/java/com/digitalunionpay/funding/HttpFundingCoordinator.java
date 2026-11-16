package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用出入金服务 8086 通知法币到账。 */
public final class HttpFundingCoordinator implements FundingCoordinator {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpFundingCoordinator(RestClient client) {
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

  /** 调用出入金服务的 /internal/funding/fiat-settlements，不读取响应体。 */
  @Override
  public void onFiatSettled(String userId, Money amount, String providerReference) {
    client.post()
        .uri(FundingPaths.FIAT_SETTLEMENTS)
        .body(new FiatSettled(userId, amount, providerReference))
        .retrieve()
        .toBodilessEntity();
  }
}
