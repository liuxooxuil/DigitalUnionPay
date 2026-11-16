package com.digitalunionpay.funding.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.funding.mapper.FiatSettlementMapper;
import com.digitalunionpay.funding.service.FundingSettleService;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.user.KycGate;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class FundingSettleServiceImpl implements FundingSettleService {

  private final KycGate kycGate;
  private final RiskGate riskGate;
  private final TccLedger ledger;
  private final NotificationPort notifications;
  private final FiatSettlementMapper fiatSettlementMapper;

  /** 接入协作者和本聚合的 Mapper。 */
  public FundingSettleServiceImpl(
      KycGate kycGate, RiskGate riskGate, TccLedger ledger, NotificationPort notifications,
      FiatSettlementMapper fiatSettlementMapper) {
    if (kycGate == null || riskGate == null || ledger == null || notifications == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.kycGate = kycGate;
    this.riskGate = riskGate;
    this.ledger = ledger;
    this.notifications = notifications;
    this.fiatSettlementMapper = fiatSettlementMapper;
  }

  /** 开启 Seata 全局事务后执行入账辅助方法，尚未按 KYC、风控、清结算 TCC 的顺序入账。 */
  @Override
  public void onFiatSettled(String userId, Money amount, String providerReference) {
    GlobalTransactions.run("funding.onFiatSettled", () -> settle(userId, amount, providerReference));
  }

  /** 校验入参和协作者，业务尚未实现。 */
  private void settle(String userId, Money amount, String providerReference) {
    Ids.required(userId, "userId");
    Ids.required(providerReference, "providerReference");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
    if (kycGate == null || riskGate == null || ledger == null || notifications == null) {
      throw new IllegalStateException("collaborators");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "FundingCoordinator.onFiatSettled");
  }
}
