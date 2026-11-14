package com.digitalunionpay.valuation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.valuation.internal.SkeletonValuationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ValuationServiceContextTest {

  @Autowired
  ValuationPort valuation;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonValuationPort.class, valuation);
  }
}
