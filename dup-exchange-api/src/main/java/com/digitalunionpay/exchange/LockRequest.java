package com.digitalunionpay.exchange;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record LockRequest(String userId, Money source, String targetCurrency) {

  public LockRequest {
    userId = Ids.required(userId, "userId");
    targetCurrency = Ids.required(targetCurrency, "targetCurrency");
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
  }
}
