package com.digitalunionpay.valuation;

import com.digitalunionpay.common.money.Money;

/** 兑换和入金锁价使用的报价。不在这里改余额。 */
public interface ValuationPort {

  /** 按报价把源金额换成目标币种。 */
  Money convert(Money source, String targetCurrency);
}
