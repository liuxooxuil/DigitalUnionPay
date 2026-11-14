package com.digitalunionpay.trading;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradingController {

  private final TradingGateway trading;

  public TradingController(TradingGateway trading) {
    this.trading = trading;
  }

  @PostMapping(TradingPaths.MARKET_ORDERS)
  public OrderTicket openMarket(@RequestBody OpenMarketOrder request) {
    return new OrderTicket(trading.openMarket(request.userId(), request.symbol(), request.side(), request.lots()));
  }
}
