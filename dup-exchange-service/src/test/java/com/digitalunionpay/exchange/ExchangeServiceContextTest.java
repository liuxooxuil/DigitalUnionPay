package com.digitalunionpay.exchange;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.exchange.internal.SkeletonExchangePort;
import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeServiceContextTest {

  @Autowired
  ExchangePort exchange;

  @Autowired
  ValuationPort valuation;

  @Autowired
  RiskGate riskGate;

  @Autowired
  TccLedger ledger;

  @Autowired
  NotificationPort notifications;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonExchangePort.class, exchange);
    assertInstanceOf(HttpValuationPort.class, valuation);
    assertInstanceOf(HttpRiskGate.class, riskGate);
    assertInstanceOf(HttpTccLedger.class, ledger);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }
}
