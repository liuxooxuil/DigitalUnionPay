package com.digitalunionpay.trading;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.trading.service.impl.MarketOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查自营交易服务上下文：网关是骨架，清结算和风控是 HTTP 客户端，通知走 RocketMQ。 */
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

  /** 断言网关为骨架，清结算和风控为 HTTP 客户端，通知为 RocketMQ 端口。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(MarketOrderServiceImpl.class, trading);
    assertInstanceOf(HttpTccLedger.class, ledger);
    assertInstanceOf(HttpRiskGate.class, riskGate);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }
}
