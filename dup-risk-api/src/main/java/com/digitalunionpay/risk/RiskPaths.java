package com.digitalunionpay.risk;

/** 风控服务对外的内部 HTTP 路径。 */
public final class RiskPaths {
  /** 同步风控检查。 */
  public static final String CHECKS = "/internal/risk/checks";

  /** 禁止实例化。 */
  private RiskPaths() {
  }
}
