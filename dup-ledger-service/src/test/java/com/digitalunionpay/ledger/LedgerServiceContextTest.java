package com.digitalunionpay.ledger;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.ledger.service.impl.LedgerServiceImpl;
import com.digitalunionpay.support.TraceIdFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 检查清结算上下文、同事务入账、重复入账、空回滚悬挂，以及不平衡分录的 422。 */
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
  JdbcTemplate jdbc;

  @Autowired
  TraceIdFilter traceIdFilter;

  @Autowired
  Filter springSecurityFilterChain;

  private MockMvc mvc;

  /** 装上链路号和 Spring Security，并清掉上一笔用例留下的账和幂等键。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter, springSecurityFilterChain).build();
    jdbc.update("DELETE FROM ledger_posting");
    jdbc.update("DELETE FROM ledger_journal");
    jdbc.update("DELETE FROM ledger_freeze");
    jdbc.update("DELETE FROM dup_outbox");
    jdbc.update("DELETE FROM dup_idempotency");
    jdbc.update("DELETE FROM dup_tcc_fence");
  }

  /** 确认账本端口是本地占位实现。 */
  @Test
  void exposesTheLocalLedger() {
    assertInstanceOf(LedgerServiceImpl.class, ledger);
  }

  /** 平衡分录、幂等键和本地消息在同一次提交里写成。 */
  @Test
  void balancedJournalIsPostedWithItsOutboxRow() throws Exception {
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceIdFilter.HEADER, "trace-1")
            .content(BALANCED))
        .andExpect(status().isOk())
        .andExpect(header().string(TraceIdFilter.HEADER, "trace-1"));
    Integer journals = jdbc.queryForObject(
        "SELECT COUNT(*) FROM ledger_journal WHERE reference = 'ref-1'", Integer.class);
    Integer postings = jdbc.queryForObject(
        "SELECT COUNT(*) FROM ledger_posting WHERE journal_id IN (SELECT id FROM ledger_journal WHERE reference = 'ref-1')",
        Integer.class);
    Integer messages = jdbc.queryForObject(
        "SELECT COUNT(*) FROM dup_outbox WHERE idempotency_key = 'ref-1'", Integer.class);
    org.junit.jupiter.api.Assertions.assertEquals(1, journals);
    org.junit.jupiter.api.Assertions.assertEquals(2, postings);
    org.junit.jupiter.api.Assertions.assertEquals(1, messages);
  }

  /** 同一个 reference 再提交一次返回冲突，不再插第二笔分录。 */
  @Test
  void duplicateJournalIsConflict() throws Exception {
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceIdFilter.HEADER, "trace-1")
            .content(BALANCED))
        .andExpect(status().isOk());
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceIdFilter.HEADER, "trace-1")
            .content(BALANCED))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  /** Cancel 先到只记空回滚，随后的 Try 按悬挂拒绝。 */
  @Test
  void cancelBeforeTryHangs() throws Exception {
    mvc.perform(post("/internal/ledger/freezes/freeze-1/cancel"))
        .andExpect(status().isOk());
    mvc.perform(post(LedgerPaths.FREEZES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"traceId\":\"trace-1\",\"reference\":\"freeze-1\",\"accountId\":\"customer-myr\",\"amount\":{\"minorUnits\":1000,\"scale\":2,\"currency\":\"MYR\"}}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("hanging"));
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
        .andExpect(jsonPath("$.code").value("UNBALANCED_JOURNAL"))
        .andExpect(jsonPath("$.traceId").value("trace-1"));
  }

  /** 请求体不是 JSON 时返回 400，不把解析细节回给调用方。 */
  @Test
  void malformedJsonIsBadRequest() throws Exception {
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceIdFilter.HEADER, "trace-bad")
            .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.traceId").value("trace-bad"));
  }
}
