package com.digitalunionpay.exchange.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.exchange.ExchangePort;
import com.digitalunionpay.exchange.PriceLock;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.valuation.ValuationPort;

public final class SkeletonExchangePort implements ExchangePort {

  private final ValuationPort valuation;
  private final RiskGate riskGate;
  private final TccLedger ledger;
  private final NotificationPort notifications;

  public SkeletonExchangePort(
      ValuationPort valuation, RiskGate riskGate, TccLedger ledger, NotificationPort notifications) {
    if (valuation == null || riskGate == null || ledger == null || notifications == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.valuation = valuation;
    this.riskGate = riskGate;
    this.ledger = ledger;
    this.notifications = notifications;
  }

  @Override
  public PriceLock lock(String userId, Money source, String targetCurrency) {
    GlobalTransactions.run("exchange.lock", () -> lockQuote(userId, source, targetCurrency));
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "ExchangePort.lock");
  }

  private void lockQuote(String userId, Money source, String targetCurrency) {
    Ids.required(userId, "userId");
    Ids.required(targetCurrency, "targetCurrency");
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
    if (valuation == null || riskGate == null || ledger == null || notifications == null) {
      throw new IllegalStateException("collaborators");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "ExchangePort.lock");
  }
}
