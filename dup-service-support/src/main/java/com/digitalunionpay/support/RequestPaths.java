package com.digitalunionpay.support;

import jakarta.servlet.http.HttpServletRequest;

/** 去掉 context path，权限和签名都按这个路径算。 */
final class RequestPaths {

  /** 禁止实例化。 */
  private RequestPaths() {
  }

  /** 没有 context path 时原样返回。 */
  static String of(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    if (context != null && !context.isEmpty() && uri.startsWith(context)) {
      return uri.substring(context.length());
    }
    return uri;
  }
}
