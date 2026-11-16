package com.digitalunionpay.matching;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.matching.service.impl.MatchOrderServiceImpl;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查撮合服务上下文：订单簿是服务实现，估值和清结算是 HTTP 客户端。 */
@SpringBootTest
@ActiveProfiles("test")
class MatchingServiceContextTest {

  @Autowired
  OrderBook orderBook;

  @Autowired
  ValuationPort valuation;

  @Autowired
  TccLedger ledger;

  /** 断言订单簿是服务实现，估值和清结算是 HTTP 客户端。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(MatchOrderServiceImpl.class, orderBook);
    assertInstanceOf(HttpValuationPort.class, valuation);
    assertInstanceOf(HttpTccLedger.class, ledger);
  }
}
