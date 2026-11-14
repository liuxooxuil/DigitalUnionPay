package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;

public record SignRequest(String payloadRef) {

  public SignRequest {
    payloadRef = Ids.required(payloadRef, "payloadRef");
  }
}
