package com.digitalunionpay.physical;

import com.digitalunionpay.common.Ids;

public record IntakeId(String value) {

  public IntakeId {
    value = Ids.required(value, "value");
  }
}
