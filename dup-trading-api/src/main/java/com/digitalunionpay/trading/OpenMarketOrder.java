package com.digitalunionpay.trading;

import com.digitalunionpay.common.Ids;

public record OpenMarketOrder(String userId, String symbol, String side, long lots) {

  public OpenMarketOrder {
    userId = Ids.required(userId, "userId");
    symbol = Ids.required(symbol, "symbol");
    side = Ids.required(side, "side");
    if (lots <= 0) {
      throw new IllegalArgumentException("lots");
    }
  }
}
