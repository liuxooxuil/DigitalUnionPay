package com.digitalunionpay.support;

/** 按访问 token 判断当前请求能不能打到这个方法和路径。 */
public interface AccessChecker {

  /** token 无效时抛 UNAUTHENTICATED，没有权限时抛 FORBIDDEN。 */
  void check(String token, String method, String path);
}
