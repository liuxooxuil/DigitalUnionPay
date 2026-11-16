package com.digitalunionpay.risk;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.risk.service.impl.RiskCheckServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查风控服务上下文是否装上本地风控端口。 */
@SpringBootTest
@ActiveProfiles("test")
class RiskServiceContextTest {

  @Autowired
  RiskGate riskGate;

  /** 确认风控端口是占位实现。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(RiskCheckServiceImpl.class, riskGate);
  }
}
