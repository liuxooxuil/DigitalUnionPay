package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 出金申请。用户、金额和收款人引用都要有。 */
public record SubmitWithdrawal(String userId, Money amount, String beneficiaryRef) {

  /** 拒绝空的用户、收款人引用和金额。 */
  public SubmitWithdrawal {
    userId = Ids.required(userId, "userId");
    beneficiaryRef = Ids.required(beneficiaryRef, "beneficiaryRef");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
