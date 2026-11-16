package com.digitalunionpay.exchange.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.exchange.PriceLock;
import com.digitalunionpay.exchange.mapper.PriceLockMapper;
import com.digitalunionpay.exchange.service.ExchangeQuoteService;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.valuation.ValuationPort;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class ExchangeQuoteServiceImpl implements ExchangeQuoteService {

  private final ValuationPort valuation;
  private final RiskGate riskGate;
  private final TccLedger ledger;
  private final NotificationPort notifications;
  private final PriceLockMapper priceLockMapper;

  /** 接入协作者和本聚合的 Mapper。 */
  public ExchangeQuoteServiceImpl(
      ValuationPort valuation, RiskGate riskGate, TccLedger ledger, NotificationPort notifications,
      PriceLockMapper priceLockMapper) {
    if (valuation == null || riskGate == null || ledger == null || notifications == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.valuation = valuation;
    this.riskGate = riskGate;
    this.ledger = ledger;
    this.notifications = notifications;
    this.priceLockMapper = priceLockMapper;
  }

  /** 开启 Seata 全局事务后执行锁价辅助方法，事务正常返回时仍抛出 PORT_NOT_IMPLEMENTED。 */
  @Override
  public PriceLock lock(String userId, Money source, String targetCurrency) {
    GlobalTransactions.run("exchange.lock", () -> lockQuote(userId, source, targetCurrency));
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "ExchangePort.lock");
  }

  /** 校验入参和协作者，业务尚未实现。 */
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
