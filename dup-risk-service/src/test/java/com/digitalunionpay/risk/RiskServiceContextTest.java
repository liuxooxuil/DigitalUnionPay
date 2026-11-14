package com.digitalunionpay.risk;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.risk.internal.SkeletonRiskGate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RiskServiceContextTest {

  @Autowired
  RiskGate riskGate;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonRiskGate.class, riskGate);
  }
}
