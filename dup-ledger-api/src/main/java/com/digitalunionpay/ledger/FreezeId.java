package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;

/** 一次预冻结的编号。 */
public record FreezeId(String value) {

  /** 拒绝空白编号。 */
  public FreezeId {
    value = Ids.required(value, "freezeId");
  }
}
