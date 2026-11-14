package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;

public record WithdrawalId(String value) {

  public WithdrawalId {
    value = Ids.required(value, "value");
  }
}
