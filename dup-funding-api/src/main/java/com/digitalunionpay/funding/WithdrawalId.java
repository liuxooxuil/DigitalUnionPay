package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;

/** 出金申请号。 */
public record WithdrawalId(String value) {

  /** 拒绝空的申请号。 */
  public WithdrawalId {
    value = Ids.required(value, "value");
  }
}
