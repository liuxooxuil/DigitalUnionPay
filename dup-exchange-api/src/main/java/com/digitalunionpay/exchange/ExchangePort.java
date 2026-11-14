package com.digitalunionpay.exchange;

import com.digitalunionpay.common.money.Money;

/** 法币与稳定币的双向兑换。成交前先取估值并过风控。 */
public interface ExchangePort {

  PriceLock lock(String userId, Money source, String targetCurrency);
}
