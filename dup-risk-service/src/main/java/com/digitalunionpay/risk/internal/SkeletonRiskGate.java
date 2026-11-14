package com.digitalunionpay.risk.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.risk.RiskAction;
import com.digitalunionpay.risk.RiskDecision;
import com.digitalunionpay.risk.RiskGate;

public final class SkeletonRiskGate implements RiskGate {

  @Override
  public RiskDecision check(String userId, RiskAction action, Money amount) {
    Ids.required(userId, "userId");
    if (action == null || amount == null) {
      throw new IllegalArgumentException("action and amount");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "RiskGate.check");
  }
}
