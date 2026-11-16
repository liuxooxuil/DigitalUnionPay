package com.digitalunionpay.user;

import com.digitalunionpay.common.Ids;
import org.springframework.web.client.RestClient;

/** 调用用户服务 8081 的 KycGate。 */
public final class HttpKycGate implements KycGate {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpKycGate(RestClient client) {
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

  /** 调用 GET /internal/users/{userId}/funding-allowed；空响应视为不允许。 */
  @Override
  public boolean fundingAllowed(String userId) {
    Ids.required(userId, "userId");
    return Boolean.TRUE.equals(client.get().uri(UserPaths.FUNDING_ALLOWED, userId).retrieve().body(Boolean.class));
  }
}
