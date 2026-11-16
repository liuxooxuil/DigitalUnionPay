package com.digitalunionpay.matching;

import com.digitalunionpay.common.money.Money;

/** 订单簿。成交后的资金结算交给清结算。 */
public interface OrderBook {

  /** 接受一笔订单进入订单簿。 */
  void accept(String orderId, String pair, String side, Money price, long quantityMinor);
}
