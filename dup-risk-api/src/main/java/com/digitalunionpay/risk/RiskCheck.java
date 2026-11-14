package com.digitalunionpay.risk;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record RiskCheck(String userId, RiskAction action, Money amount) {

  public RiskCheck {
    userId = Ids.required(userId, "userId");
    if (action == null || amount == null) {
      throw new IllegalArgumentException("action and amount");
    }
  }
}
