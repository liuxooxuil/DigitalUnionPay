package com.digitalunionpay.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.support.TraceIdFilter;
import com.digitalunionpay.user.service.impl.InheritanceCaseServiceImpl;
import com.digitalunionpay.user.service.impl.KycServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 检查用户服务上下文、出入金接口的 501，以及链路号是否回写。 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceContextTest {

  @Autowired
  KycGate kycGate;

  @Autowired
  InheritanceCasePort inheritance;

  @Autowired
  NotificationPort notifications;

  @Autowired
  WebApplicationContext context;

  @Autowired
  TraceIdFilter traceIdFilter;

  private MockMvc mvc;

  /** 装上链路号过滤器。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter).build();
  }

  /** 确认 KYC 和继承是占位实现，通知走 RocketMQ。 */
  @Test
  void keepsKycLocalAndSendsNotificationsOverHttp() {
    assertInstanceOf(KycServiceImpl.class, kycGate);
    assertInstanceOf(InheritanceCaseServiceImpl.class, inheritance);
    assertInstanceOf(RocketNotificationPort.class, notifications);
  }

  /** 带链路号查询出入金资格时返回 501，并回写原链路号。 */
  @Test
  void fundingCheckIsRejectedUntilKycIsImplemented() throws Exception {
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, "user-1").header(TraceIdFilter.HEADER, "trace-kyc"))
        .andExpect(status().is(501))
        .andExpect(header().string(TraceIdFilter.HEADER, "trace-kyc"))
        .andExpect(jsonPath("$.code").value("PORT_NOT_IMPLEMENTED"));
  }

  /** 未带链路号时响应里写出非空的 X-Trace-Id。 */
  @Test
  void missingTraceHeaderIsCreated() throws Exception {
    String traceId = mvc.perform(get(UserPaths.FUNDING_ALLOWED, "user-1"))
        .andExpect(status().is(501))
        .andReturn()
        .getResponse()
        .getHeader(TraceIdFilter.HEADER);
    assertNotNull(traceId);
    assertFalse(traceId.isBlank());
  }
}
