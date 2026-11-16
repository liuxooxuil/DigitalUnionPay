package com.digitalunionpay.funding;

/** 出入金服务的内部 HTTP 路径。 */
public final class FundingPaths {
  /** 签发虚拟 IBAN。 */
  public static final String VIRTUAL_IBANS = "/internal/funding/virtual-ibans";
  /** 提交出金申请。 */
  public static final String WITHDRAWALS = "/internal/funding/withdrawals";
  /** 通知法币到账。 */
  public static final String FIAT_SETTLEMENTS = "/internal/funding/fiat-settlements";

  /** 禁止实例化。 */
  private FundingPaths() {
  }
}
