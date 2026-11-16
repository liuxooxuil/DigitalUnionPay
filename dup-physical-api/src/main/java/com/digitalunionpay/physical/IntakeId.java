package com.digitalunionpay.physical;

import com.digitalunionpay.common.Ids;

/** 一次实物入金的编号。 */
public record IntakeId(String value) {

  /** 拒绝 null 和空白的入金号。 */
  public IntakeId {
    value = Ids.required(value, "value");
  }
}
