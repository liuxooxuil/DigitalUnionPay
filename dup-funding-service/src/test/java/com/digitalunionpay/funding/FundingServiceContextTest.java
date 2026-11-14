package com.digitalunionpay.funding;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.funding.internal.SkeletonFiatCollectionPort;
import com.digitalunionpay.funding.internal.SkeletonFundingCoordinator;
import com.digitalunionpay.funding.internal.SkeletonWithdrawalPort;
import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.user.HttpKycGate;
import com.digitalunionpay.user.KycGate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FundingServiceContextTest {

  @Autowired
  FiatCollectionPort fiat;

  @Autowired
  WithdrawalPort withdrawals;

  @Autowired
  FundingCoordinator coordinator;

  @Autowired
  KycGate kycGate;

  @Autowired
  RiskGate riskGate;

  @Autowired
  TccLedger ledger;

  @Autowired
  NotificationPort notifications;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonFiatCollectionPort.class, fiat);
    assertInstanceOf(SkeletonWithdrawalPort.class, withdrawals);
    assertInstanceOf(SkeletonFundingCoordinator.class, coordinator);
    assertInstanceOf(HttpKycGate.class, kycGate);
    assertInstanceOf(HttpRiskGate.class, riskGate);
    assertInstanceOf(HttpTccLedger.class, ledger);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }
}
