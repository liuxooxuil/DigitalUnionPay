package com.digitalunionpay.user;

/** 用户服务对外的内部 HTTP 路径。 */
public final class UserPaths {
  /** 查询用户是否允许出入金。 */
  public static final String FUNDING_ALLOWED = "/internal/users/{userId}/funding-allowed";
  /** 打开继承案件。 */
  public static final String INHERITANCE_CASES = "/internal/inheritance-cases";

  /** 复核一笔继承案件。 */
  public static final String INHERITANCE_REVIEW = "/internal/inheritance-cases/{caseId}/reviews";

  /** 当前登录用户的档案。 */
  public static final String ME = "/internal/users/me";

  /** 当前用户的多币种余额。 */
  public static final String ME_BALANCES = "/internal/users/me/balances";

  /** 当前用户的流水。 */
  public static final String ME_STATEMENTS = "/internal/users/me/statements";

  /** 按 KYC 等级检查单笔和累计限额。 */
  public static final String ME_LIMITS = "/internal/users/me/limits";

  /** 绑定同名收款账户。 */
  public static final String ME_BANK = "/internal/users/me/bank-accounts";

  /** 申请关闭账户。 */
  public static final String ME_CLOSURE = "/internal/users/me/closure";

  /** 人工审核关闭申请。不在客户权限 /internal/users/** 下面。 */
  public static final String CLOSURE_DECISION = "/internal/closures/{userId}/decision";

  /** 当前用户的 KYC。 */
  public static final String ME_KYC = "/internal/users/me/kyc";

  /** 上传证件的一面。 */
  public static final String ME_KYC_DOCUMENTS = "/internal/users/me/kyc/documents";

  /** 对刚上传的内容做 OCR。 */
  public static final String ME_KYC_OCR = "/internal/users/me/kyc/ocr";

  /** 签发一次活体会话。 */
  public static final String ME_KYC_LIVENESS = "/internal/users/me/kyc/liveness";

  /** 审核 KYC。不在客户权限 /internal/users/** 下面。 */
  public static final String KYC_DECISION = "/internal/kyc-cases/{userId}/decision";

  /** 当前用户仍在线的设备。 */
  public static final String SESSIONS = "/internal/sessions";

  /** 远程下线一台设备。 */
  public static final String SESSION = "/internal/sessions/{sessionId}";

  /** 禁止实例化。 */
  private UserPaths() {
  }
}
