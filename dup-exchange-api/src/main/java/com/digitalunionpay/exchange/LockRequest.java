package com.digitalunionpay.exchange;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 锁价请求。用户、源金额和目标币种都要有。 */
public record LockRequest(String userId, Money source, String targetCurrency) {

  /** 拒绝空的用户、目标币种和源金额。 */
  public LockRequest {
    userId = Ids.required(userId, "userId");
    targetCurrency = Ids.required(targetCurrency, "targetCurrency");
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
  }
}
