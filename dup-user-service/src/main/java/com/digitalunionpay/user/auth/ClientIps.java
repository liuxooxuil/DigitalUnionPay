package com.digitalunionpay.user.auth;

import jakarta.servlet.http.HttpServletRequest;

/** 取调用方 IP。边缘网关会把第一跳放进 X-Forwarded-For。 */
public final class ClientIps {

  /** 禁止实例化。 */
  private ClientIps() {
  }

  /** 有转发头时用第一跳，否则用连接地址。 */
  public static String of(HttpServletRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request");
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      String first = comma < 0 ? forwarded : forwarded.substring(0, comma);
      if (!first.isBlank()) {
        return first.trim();
      }
    }
    String remote = request.getRemoteAddr();
    return remote == null || remote.isBlank() ? "0.0.0.0" : remote;
  }
}
