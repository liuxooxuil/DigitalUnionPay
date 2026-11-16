package com.digitalunionpay.user;

/** 用户服务对外的内部 HTTP 路径。 */
public final class UserPaths {
  /** 查询用户是否允许出入金。 */
  public static final String FUNDING_ALLOWED = "/internal/users/{userId}/funding-allowed";
  /** 打开继承案件。 */
  public static final String INHERITANCE_CASES = "/internal/inheritance-cases";

  /** 禁止实例化。 */
  private UserPaths() {
  }
}
