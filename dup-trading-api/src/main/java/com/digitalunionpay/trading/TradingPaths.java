package com.digitalunionpay.trading;

/** 自营交易服务的内部 HTTP 路径。 */
public final class TradingPaths {
  /** 开一笔市价单。 */
  public static final String MARKET_ORDERS = "/internal/trading/market-orders";

  /** 禁止实例化。 */
  private TradingPaths() {
  }
}
