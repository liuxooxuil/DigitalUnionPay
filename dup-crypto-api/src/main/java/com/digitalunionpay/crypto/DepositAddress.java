package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;

/** 链上充值地址。 */
public record DepositAddress(String value) {

  /** 拒绝 null 和空白地址。 */
  public DepositAddress {
    value = Ids.required(value, "value");
  }
}
