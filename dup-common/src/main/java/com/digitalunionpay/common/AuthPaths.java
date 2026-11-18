package com.digitalunionpay.common;

/** 注册、登录和鉴权这几个内部路径。各服务的 Spring Security 链也放行这几条。 */
public final class AuthPaths {

  /** 发送短信或邮箱验证码。不回明文。 */
  public static final String VERIFICATION_CODES = "/internal/auth/verification-codes";

  /** 用用户名和密码新建用户。不签发 token。 */
  public static final String REGISTER = "/internal/auth/register";

  /** 用用户名和密码换访问 token。 */
  public static final String LOGIN = "/internal/auth/login";

  /** 用短信或邮箱验证码换访问 token。 */
  public static final String LOGIN_CODE = "/internal/auth/login/code";

  /** 用已绑定设备的生物识别签名换访问 token。 */
  public static final String LOGIN_BIOMETRIC = "/internal/auth/login/biometric";

  /** 签发一次性生物识别挑战。 */
  public static final String BIOMETRIC_CHALLENGES = "/internal/auth/biometric/challenges";

  /** 按当前 token 检查能否访问某个方法和路径。 */
  public static final String ACCESS = "/internal/auth/access";

  /** 修改登录密码。 */
  public static final String SECURITY_PASSWORD = "/internal/auth/security/password";

  /** 绑定或解绑手机号、邮箱。 */
  public static final String SECURITY_CONTACTS = "/internal/auth/security/contacts";

  /** 打开或关闭二次验证。 */
  public static final String SECURITY_MFA = "/internal/auth/security/mfa";

  /** 绑定当前会话设备的生物识别公钥。 */
  public static final String SECURITY_DEVICES = "/internal/auth/security/devices";

  /** 禁止实例化。 */
  private AuthPaths() {
  }
}
