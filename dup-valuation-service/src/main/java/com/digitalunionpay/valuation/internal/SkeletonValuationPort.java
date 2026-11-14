package com.digitalunionpay.valuation.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.valuation.ValuationPort;

public final class SkeletonValuationPort implements ValuationPort {

  @Override
  public Money convert(Money source, String targetCurrency) {
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
    Ids.required(targetCurrency, "targetCurrency");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "ValuationPort.convert");
  }
}
