package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;

/** 渠道返回的虚拟 IBAN。 */
public record IssuedIban(String value) {

  /** 拒绝空的账号。 */
  public IssuedIban {
    value = Ids.required(value, "value");
  }
}
