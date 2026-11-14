package com.digitalunionpay.matching;

import com.digitalunionpay.common.money.Money;

public interface OrderBook {

  void accept(String orderId, String pair, String side, Money price, long quantityMinor);
}
