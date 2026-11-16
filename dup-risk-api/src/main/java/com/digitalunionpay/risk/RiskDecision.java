package com.digitalunionpay.risk;

/** 风控是否放行，以及等级和原因。 */
public record RiskDecision(boolean allowed, RiskLevel level, String reason) {

  /** 拒绝空等级和空白原因。 */
  public RiskDecision {
    if (level == null) {
      throw new IllegalArgumentException("level");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason");
    }
  }
}
