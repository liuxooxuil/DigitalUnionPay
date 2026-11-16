package com.digitalunionpay.ledger;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.ledger.service.impl.LedgerServiceImpl;
import com.digitalunionpay.support.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 检查清结算上下文、平衡分录的 501，以及不平衡分录的 422。 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerServiceContextTest {

  private static final String BALANCED = "{\"traceId\":\"trace-1\",\"reference\":\"ref-1\",\"postings\":["
      + "{\"accountId\":\"customer-myr\",\"side\":\"DEBIT\",\"amount\":{\"minorUnits\":1000,\"scale\":2,\"currency\":\"MYR\"}},"
      + "{\"accountId\":\"nostro-myr\",\"side\":\"CREDIT\",\"amount\":{\"minorUnits\":1000,\"scale\":2,\"currency\":\"MYR\"}}]}";

  private static final String UNBALANCED = "{\"traceId\":\"trace-1\",\"reference\":\"ref-1\",\"postings\":["
      + "{\"accountId\":\"customer-myr\",\"side\":\"DEBIT\",\"amount\":{\"minorUnits\":1000,\"scale\":2,\"currency\":\"MYR\"}},"
      + "{\"accountId\":\"nostro-myr\",\"side\":\"CREDIT\",\"amount\":{\"minorUnits\":900,\"scale\":2,\"currency\":\"MYR\"}}]}";

  @Autowired
  TccLedger ledger;

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

  /** 确认账本端口是本地占位实现。 */
  @Test
  void exposesTheLocalLedger() {
    assertInstanceOf(LedgerServiceImpl.class, ledger);
  }

  /** 平衡分录经 HTTP 提交后返回 501，并回写原链路号。 */
  @Test
  void balancedJournalCrossesTheHttpBoundaryAndStaysUnimplemented() throws Exception {
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceIdFilter.HEADER, "trace-1")
            .content(BALANCED))
        .andExpect(status().is(501))
        .andExpect(header().string(TraceIdFilter.HEADER, "trace-1"))
        .andExpect(jsonPath("$.code").value("PORT_NOT_IMPLEMENTED"));
  }

  /** 不平衡分录在入账前返回 422 和 UNBALANCED_JOURNAL。 */
  @Test
  void unbalancedJournalIsRejectedBeforePosting() throws Exception {
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceIdFilter.HEADER, "trace-1")
            .content(UNBALANCED))
        .andExpect(status().is(422))
        .andExpect(header().string(TraceIdFilter.HEADER, "trace-1"))
        .andExpect(jsonPath("$.code").value("UNBALANCED_JOURNAL"));
  }
}
