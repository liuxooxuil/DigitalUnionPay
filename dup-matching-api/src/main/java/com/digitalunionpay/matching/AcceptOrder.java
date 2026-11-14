package com.digitalunionpay.matching;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record AcceptOrder(String orderId, String pair, String side, Money price, long quantityMinor) {

  public AcceptOrder {
    orderId = Ids.required(orderId, "orderId");
    pair = Ids.required(pair, "pair");
    side = Ids.required(side, "side");
    if (price == null || quantityMinor <= 0) {
      throw new IllegalArgumentException("price and quantity");
    }
  }
}
