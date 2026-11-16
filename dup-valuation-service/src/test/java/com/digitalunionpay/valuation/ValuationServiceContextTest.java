package com.digitalunionpay.valuation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.valuation.service.impl.QuoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查估值服务上下文是否装上本地估值端口。 */
@SpringBootTest
@ActiveProfiles("test")
class ValuationServiceContextTest {

  @Autowired
  ValuationPort valuation;

  /** 确认估值端口是占位实现。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(QuoteServiceImpl.class, valuation);
  }
}
