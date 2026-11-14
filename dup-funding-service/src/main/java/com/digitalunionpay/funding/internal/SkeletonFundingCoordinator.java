package com.digitalunionpay.funding.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.funding.FundingCoordinator;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.user.KycGate;

public final class SkeletonFundingCoordinator implements FundingCoordinator {

  private final KycGate kycGate;
  private final RiskGate riskGate;
  private final TccLedger ledger;
  private final NotificationPort notifications;

  public SkeletonFundingCoordinator(
      KycGate kycGate, RiskGate riskGate, TccLedger ledger, NotificationPort notifications) {
    if (kycGate == null || riskGate == null || ledger == null || notifications == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.kycGate = kycGate;
    this.riskGate = riskGate;
    this.ledger = ledger;
    this.notifications = notifications;
  }

  @Override
  public void onFiatSettled(String userId, Money amount, String providerReference) {
    GlobalTransactions.run("funding.onFiatSettled", () -> settle(userId, amount, providerReference));
  }

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
