package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;

public record DepositAddress(String value) {

  public DepositAddress {
    value = Ids.required(value, "value");
  }
}
