package com.digitalunionpay.trading.controller;

import com.digitalunionpay.trading.OpenMarketOrder;
import com.digitalunionpay.trading.OrderTicket;
import com.digitalunionpay.trading.TradingPaths;
import com.digitalunionpay.trading.service.MarketOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class TradingController {

  private final MarketOrderService trading;

  /** 接入本进程的端口实现。 */
  public TradingController(MarketOrderService trading) {
    this.trading = trading;
  }

  /** 处理 POST /internal/trading/market-orders，返回订单回执。 */
  @PostMapping(TradingPaths.MARKET_ORDERS)
  public OrderTicket openMarket(@RequestBody OpenMarketOrder request) {
    return new OrderTicket(trading.openMarket(request.userId(), request.symbol(), request.side(), request.lots()));
  }
}
