package com.digitalunionpay.exchange;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.exchange.service.impl.ExchangeQuoteServiceImpl;
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

/** 检查兑换服务上下文：兑换端口是服务实现，估值、风控和清结算是 HTTP 客户端，通知走 RocketMQ。 */
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

  /** 断言兑换端口是服务实现，估值、风控和清结算是 HTTP 客户端，通知是 RocketMQ。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(ExchangeQuoteServiceImpl.class, exchange);
    assertInstanceOf(HttpValuationPort.class, valuation);
    assertInstanceOf(HttpRiskGate.class, riskGate);
    assertInstanceOf(HttpTccLedger.class, ledger);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }
}
