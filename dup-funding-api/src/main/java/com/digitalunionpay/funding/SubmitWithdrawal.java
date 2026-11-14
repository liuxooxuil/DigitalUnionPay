package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record SubmitWithdrawal(String userId, Money amount, String beneficiaryRef) {

  public SubmitWithdrawal {
    userId = Ids.required(userId, "userId");
    beneficiaryRef = Ids.required(beneficiaryRef, "beneficiaryRef");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
