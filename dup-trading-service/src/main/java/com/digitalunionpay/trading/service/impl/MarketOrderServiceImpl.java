package com.digitalunionpay.trading.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.trading.mapper.MarketOrderMapper;
import com.digitalunionpay.trading.service.MarketOrderService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class MarketOrderServiceImpl implements MarketOrderService {

  private final TccLedger ledger;
  private final RiskGate riskGate;
  private final NotificationPort notifications;
  private final MarketOrderMapper orders;

  /** 接入协作者和本聚合的 Mapper。 */
  public MarketOrderServiceImpl(
      TccLedger ledger, RiskGate riskGate, NotificationPort notifications, MarketOrderMapper orders) {
    if (ledger == null || riskGate == null || notifications == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.ledger = ledger;
    this.riskGate = riskGate;
    this.notifications = notifications;
    this.orders = orders;
  }

  /** 开启 Seata 全局事务，然后执行开仓校验。 */
  @Override
  public String openMarket(String userId, String symbol, String side, long lots) {
    GlobalTransactions.run("trading.openMarket", () -> open(userId, symbol, side, lots));
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TradingGateway.openMarket");
  }

  /** 校验入参和协作者，业务尚未实现。 */
  private void open(String userId, String symbol, String side, long lots) {
    Ids.required(userId, "userId");
    Ids.required(symbol, "symbol");
    Ids.required(side, "side");
    if (lots <= 0) {
      throw new IllegalArgumentException("lots");
    }
    if (ledger == null || riskGate == null || notifications == null) {
      throw new IllegalStateException("collaborators");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TradingGateway.openMarket");
  }
}
