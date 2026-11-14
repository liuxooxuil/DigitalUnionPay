package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;

public record IssuedIban(String value) {

  public IssuedIban {
    value = Ids.required(value, "value");
  }
}
