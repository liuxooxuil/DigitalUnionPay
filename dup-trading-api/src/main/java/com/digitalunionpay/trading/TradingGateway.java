package com.digitalunionpay.trading;

/** 交易账户与持仓的外部网关。钱包划转仍由清结算记账。 */
public interface TradingGateway {

  /** 为用户按品种和方向开市价单，返回订单回执。 */
  String openMarket(String userId, String symbol, String side, long lots);
}
