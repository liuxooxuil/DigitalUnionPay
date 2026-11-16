package com.digitalunionpay.exchange;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;
import java.time.Instant;

/** 用户确认后的锁价。过期时间由调用方按配置传入。 */
public record PriceLock(String lockId, Money source, String targetCurrency, Instant expiresAt) {

  /** 拒绝空的锁价号、源金额、目标币种和过期时间。 */
  public PriceLock {
    lockId = Ids.required(lockId, "lockId");
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
    targetCurrency = Ids.required(targetCurrency, "targetCurrency");
    if (expiresAt == null) {
      throw new IllegalArgumentException("expiresAt");
    }
  }
}
