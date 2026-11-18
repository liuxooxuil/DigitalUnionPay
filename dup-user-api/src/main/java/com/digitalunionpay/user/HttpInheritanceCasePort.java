package com.digitalunionpay.user;

import org.springframework.web.client.RestClient;

/** 调用用户服务 8081 的 InheritanceCasePort。 */
public final class HttpInheritanceCasePort implements InheritanceCasePort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpInheritanceCasePort(RestClient client) {
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

  /** 调用 POST /internal/inheritance-cases 提交继承案件。 */
  @Override
  public void open(String caseId, String estateUserId, String heirUserId, String applicantRole, String evidenceBase64) {
    client.post()
        .uri(UserPaths.INHERITANCE_CASES)
        .body(new OpenInheritanceCase(caseId, estateUserId, heirUserId, applicantRole, evidenceBase64))
        .retrieve()
        .toBodilessEntity();
  }
}
