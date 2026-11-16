package com.digitalunionpay.valuation;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 把源金额换到目标币种的请求。 */
public record ConversionRequest(Money source, String targetCurrency) {

  /** 拒绝空的源金额和空白目标币种。 */
  public ConversionRequest {
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
    targetCurrency = Ids.required(targetCurrency, "targetCurrency");
  }
}
