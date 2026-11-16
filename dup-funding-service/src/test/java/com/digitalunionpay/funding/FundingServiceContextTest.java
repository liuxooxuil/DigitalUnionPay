package com.digitalunionpay.funding;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.funding.service.impl.FiatCollectionServiceImpl;
import com.digitalunionpay.funding.service.impl.FundingSettleServiceImpl;
import com.digitalunionpay.funding.service.impl.WithdrawalServiceImpl;
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

/** 检查出入金服务上下文：本进程端口是服务实现，用户、风控和清结算是 HTTP 客户端，通知走 RocketMQ。 */
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

  /** 断言法币渠道、出金和协调器是服务实现，KYC、风控和清结算是 HTTP 客户端，通知是 RocketMQ。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(FiatCollectionServiceImpl.class, fiat);
    assertInstanceOf(WithdrawalServiceImpl.class, withdrawals);
    assertInstanceOf(FundingSettleServiceImpl.class, coordinator);
    assertInstanceOf(HttpKycGate.class, kycGate);
    assertInstanceOf(HttpRiskGate.class, riskGate);
    assertInstanceOf(HttpTccLedger.class, ledger);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }
}
