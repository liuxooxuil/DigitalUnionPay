package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;

/** 托管签名请求，只带载荷引用。 */
public record SignRequest(String payloadRef) {

  /** 拒绝 null 和空白的载荷引用。 */
  public SignRequest {
    payloadRef = Ids.required(payloadRef, "payloadRef");
  }
}
