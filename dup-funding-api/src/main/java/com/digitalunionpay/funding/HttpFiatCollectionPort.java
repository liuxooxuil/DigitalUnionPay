package com.digitalunionpay.funding;

import org.springframework.web.client.RestClient;

/** 调用出入金服务 8086 签发虚拟 IBAN。 */
public final class HttpFiatCollectionPort implements FiatCollectionPort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpFiatCollectionPort(RestClient client) {
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

  /** 调用出入金服务的 /internal/funding/virtual-ibans。 */
  @Override
  public String issueVirtualIban(String userId, String currency) {
    IssuedIban issued = body(
        client.post().uri(FundingPaths.VIRTUAL_IBANS).body(new IssueVirtualIban(userId, currency)).retrieve().body(IssuedIban.class),
        "issuedIban");
    return issued.value();
  }
}
