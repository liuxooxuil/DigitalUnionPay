package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;

public record SignatureRef(String value) {

  public SignatureRef {
    value = Ids.required(value, "value");
  }
}
