package com.digitalunionpay.user;

/** 登录和按 RBAC 检查访问 token。 */
public interface AuthPort {

  /** 用户名和密码正确且账号有效时签发 token。clientIp 用于设备和异地判断。 */
  IssuedToken login(LoginCommand command, String clientIp);

  /** token 无效时拒绝，没有匹配权限时拒绝。 */
  void check(String token, String method, String path);
}
