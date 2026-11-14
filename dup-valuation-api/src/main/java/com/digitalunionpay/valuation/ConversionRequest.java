package com.digitalunionpay.valuation;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record ConversionRequest(Money source, String targetCurrency) {

  public ConversionRequest {
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
    targetCurrency = Ids.required(targetCurrency, "targetCurrency");
  }
}
