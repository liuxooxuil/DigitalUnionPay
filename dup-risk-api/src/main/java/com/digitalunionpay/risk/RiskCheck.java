package com.digitalunionpay.risk;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 一次风控检查的请求。 */
public record RiskCheck(String userId, RiskAction action, Money amount) {

  /** 拒绝空白用户号，以及空的动作或金额。 */
  public RiskCheck {
    userId = Ids.required(userId, "userId");
    if (action == null || amount == null) {
      throw new IllegalArgumentException("action and amount");
    }
  }
}
