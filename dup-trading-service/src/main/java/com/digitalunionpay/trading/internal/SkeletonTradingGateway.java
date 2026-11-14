package com.digitalunionpay.trading.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.trading.TradingGateway;

public final class SkeletonTradingGateway implements TradingGateway {

  private final TccLedger ledger;
  private final RiskGate riskGate;
  private final NotificationPort notifications;

  public SkeletonTradingGateway(TccLedger ledger, RiskGate riskGate, NotificationPort notifications) {
    if (ledger == null || riskGate == null || notifications == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.ledger = ledger;
    this.riskGate = riskGate;
    this.notifications = notifications;
  }

  @Override
  public String openMarket(String userId, String symbol, String side, long lots) {
    GlobalTransactions.run("trading.openMarket", () -> open(userId, symbol, side, lots));
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TradingGateway.openMarket");
  }

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
