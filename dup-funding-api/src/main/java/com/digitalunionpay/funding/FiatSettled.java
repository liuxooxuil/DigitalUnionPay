package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 法币渠道到账通知。用户、金额和渠道参考号都要有。 */
public record FiatSettled(String userId, Money amount, String providerReference) {

  /** 拒绝空的用户、渠道参考号和金额。 */
  public FiatSettled {
    userId = Ids.required(userId, "userId");
    providerReference = Ids.required(providerReference, "providerReference");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
