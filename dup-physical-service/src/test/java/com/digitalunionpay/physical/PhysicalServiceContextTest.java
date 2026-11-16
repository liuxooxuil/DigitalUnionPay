package com.digitalunionpay.physical;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.physical.service.impl.IntakeServiceImpl;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查实物入金服务上下文：入金是骨架，估值和清结算是 HTTP 客户端。 */
@SpringBootTest
@ActiveProfiles("test")
class PhysicalServiceContextTest {

  @Autowired
  PhysicalIntakePort intake;

  @Autowired
  ValuationPort valuation;

  @Autowired
  TccLedger ledger;

  /** 断言入金端口为骨架，估值和清结算为 HTTP 客户端。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(IntakeServiceImpl.class, intake);
    assertInstanceOf(HttpValuationPort.class, valuation);
    assertInstanceOf(HttpTccLedger.class, ledger);
  }
}
