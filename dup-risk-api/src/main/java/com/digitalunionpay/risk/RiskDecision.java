package com.digitalunionpay.risk;

public record RiskDecision(boolean allowed, RiskLevel level, String reason) {

  public RiskDecision {
    if (level == null) {
      throw new IllegalArgumentException("level");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason");
    }
  }
}
