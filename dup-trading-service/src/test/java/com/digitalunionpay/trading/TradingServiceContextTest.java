package com.digitalunionpay.trading;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.trading.internal.SkeletonTradingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TradingServiceContextTest {

  @Autowired
  TradingGateway trading;

  @Autowired
  TccLedger ledger;

  @Autowired
  RiskGate riskGate;

  @Autowired
  NotificationPort notifications;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonTradingGateway.class, trading);
    assertInstanceOf(HttpTccLedger.class, ledger);
    assertInstanceOf(HttpRiskGate.class, riskGate);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }
}
