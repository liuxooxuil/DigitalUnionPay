package com.digitalunionpay.common;

/** 登录和鉴权这两个内部路径。各服务的 Spring Security 链也放行这两条。 */
public final class AuthPaths {

  /** 用用户名和密码换访问 token。 */
  public static final String LOGIN = "/internal/auth/login";

  /** 按当前 token 检查能否访问某个方法和路径。 */
  public static final String ACCESS = "/internal/auth/access";

  /** 禁止实例化。 */
  private AuthPaths() {
  }
}
