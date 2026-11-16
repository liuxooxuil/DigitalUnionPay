package com.digitalunionpay.support;

import org.springframework.http.HttpRequest;

/** 出站 HTTP 用的签名器。没安装时不写签名头。 */
final class ServiceSigner {

  private static volatile ServiceCall calls;

  /** 禁止实例化。 */
  private ServiceSigner() {
  }

  /** 只在服务签名打开时装上。 */
  static void install(ServiceCall installed) {
    calls = installed;
  }

  /** 给已经组装好的出站请求补签名。 */
  static void write(HttpRequest request, byte[] body) {
    ServiceCall current = calls;
    if (current != null) {
      current.write(request, body);
    }
  }
}
