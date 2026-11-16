package com.digitalunionpay.valuation;

/** 估值服务对外的内部 HTTP 路径。 */
public final class ValuationPaths {
  /** 按源金额换算目标币种。 */
  public static final String CONVERSIONS = "/internal/valuation/conversions";

  /** 禁止实例化。 */
  private ValuationPaths() {
  }
}
