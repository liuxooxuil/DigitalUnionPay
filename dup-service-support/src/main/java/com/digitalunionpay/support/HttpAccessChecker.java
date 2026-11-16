package com.digitalunionpay.support;

import com.digitalunionpay.common.AuthPaths;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** 问用户服务这条 token 能不能访问目标接口。用户服务自己不走这条远程路径。 */
public final class HttpAccessChecker implements AccessChecker {

  private final RestClient client;

  /** 保存已经带上链路号的用户服务客户端。 */
  public HttpAccessChecker(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  /** 把目标和当前 token 交给用户服务。错误状态由客户端转成业务异常。 */
  @Override
  public void check(String token, String method, String path) {
    client.post()
        .uri(AuthPaths.ACCESS)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .body(new AccessCheckBody(method, path))
        .retrieve()
        .toBodilessEntity();
  }

  /** 和用户服务 AccessCheck 的 JSON 字段一致。 */
  private record AccessCheckBody(String method, String path) {
  }
}
