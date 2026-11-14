package com.digitalunionpay.matching;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.matching.internal.SkeletonOrderBook;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MatchingServiceContextTest {

  @Autowired
  OrderBook orderBook;

  @Autowired
  ValuationPort valuation;

  @Autowired
  TccLedger ledger;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonOrderBook.class, orderBook);
    assertInstanceOf(HttpValuationPort.class, valuation);
    assertInstanceOf(HttpTccLedger.class, ledger);
  }
}
