package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record FiatSettled(String userId, Money amount, String providerReference) {

  public FiatSettled {
    userId = Ids.required(userId, "userId");
    providerReference = Ids.required(providerReference, "providerReference");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
