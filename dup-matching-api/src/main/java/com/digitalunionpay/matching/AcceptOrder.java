package com.digitalunionpay.matching;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 送入订单簿的订单。价格不能空，数量必须为正。 */
public record AcceptOrder(String orderId, String pair, String side, Money price, long quantityMinor) {

  /** 拒绝空的订单号、交易对、方向，以及空价格或非正数量。 */
  public AcceptOrder {
    orderId = Ids.required(orderId, "orderId");
    pair = Ids.required(pair, "pair");
    side = Ids.required(side, "side");
    if (price == null || quantityMinor <= 0) {
      throw new IllegalArgumentException("price and quantity");
    }
  }
}
