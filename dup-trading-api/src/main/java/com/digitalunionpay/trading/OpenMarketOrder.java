package com.digitalunionpay.trading;

import com.digitalunionpay.common.Ids;

/** 开市价单的入参。 */
public record OpenMarketOrder(String userId, String symbol, String side, long lots) {

  /** 拒绝空白的用户、品种和方向，手数必须为正。 */
  public OpenMarketOrder {
    userId = Ids.required(userId, "userId");
    symbol = Ids.required(symbol, "symbol");
    side = Ids.required(side, "side");
    if (lots <= 0) {
      throw new IllegalArgumentException("lots");
    }
  }
}
