package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;

/** 托管签名引用。 */
public record SignatureRef(String value) {

  /** 拒绝 null 和空白的签名引用。 */
  public SignatureRef {
    value = Ids.required(value, "value");
  }
}
