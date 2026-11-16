package com.digitalunionpay.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.ledger.EntrySide;
import com.digitalunionpay.ledger.JournalCommand;
import com.digitalunionpay.ledger.Posting;
import com.digitalunionpay.ledger.service.impl.LedgerServiceImpl;
import com.digitalunionpay.support.AccessContext;
import com.digitalunionpay.support.TraceIdFilter;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
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
  JwtEncoder encoder;

  @Autowired
  TraceIdFilter traceIdFilter;

  @Autowired
  Filter springSecurityFilterChain;

  private MockMvc mvc;

  /** 装上链路号和 Spring Security，并清掉上一笔用例留下的账和幂等键。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter, springSecurityFilterChain).build();
    AccessContext.clear();
    jdbc.update("DELETE FROM ledger_posting");
    jdbc.update("DELETE FROM ledger_journal");
    jdbc.update("DELETE FROM ledger_freeze");
    jdbc.update("DELETE FROM ledger_account");
    jdbc.update("DELETE FROM dup_outbox");
    jdbc.update("DELETE FROM dup_dead_letter");
    jdbc.update("DELETE FROM dup_audit");
    jdbc.update("DELETE FROM dup_inbox");
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

  /** 已开户的账户不能被扣成负数，失败的分录整笔回滚。 */
  @Test
  void guardedDebitStopsAtAvailable() throws Exception {
    open("customer-myr", 500, "user-1");
    open("nostro-myr", 0, null);
    mvc.perform(post(LedgerPaths.JOURNALS)
            .contentType(MediaType.APPLICATION_JSON)
            .content(journal("ref-short", 1000)))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    assertEquals(0, count("SELECT COUNT(*) FROM ledger_journal"));
    assertEquals(500L, available("customer-myr"));
  }

  /** 同一业务单号第二次入账不再改余额。 */
  @Test
  void sameReferenceDoesNotMoveBalanceTwice() throws Exception {
    open("customer-myr", 2000, "user-1");
    open("nostro-myr", 0, null);
    String body = journal("ref-once", 1000);
    mvc.perform(post(LedgerPaths.JOURNALS).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    mvc.perform(post(LedgerPaths.JOURNALS).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
    assertEquals(1000L, available("customer-myr"));
    assertEquals(1, count("SELECT COUNT(*) FROM dup_audit WHERE action = 'journal.posted'"));
  }

  /** 冻结先占用可用余额，取消后回到原值。 */
  @Test
  void freezeReserveAndCancelRestoresAvailable() throws Exception {
    open("customer-myr", 1000, "user-1");
    mvc.perform(post(LedgerPaths.FREEZES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"traceId\":\"trace-1\",\"reference\":\"freeze-hold\",\"accountId\":\"customer-myr\",\"amount\":{\"minorUnits\":400,\"scale\":2,\"currency\":\"MYR\"}}"))
        .andExpect(status().isOk());
    assertEquals(600L, available("customer-myr"));
    assertEquals(400L, frozen("customer-myr"));
    mvc.perform(post("/internal/ledger/freezes/freeze-hold/cancel")).andExpect(status().isOk());
    assertEquals(1000L, available("customer-myr"));
    assertEquals(0L, frozen("customer-myr"));
  }

  /** 两笔并发扣款只有一笔能用掉仅够一次的余额。 */
  @Test
  void concurrentDebitDoesNotOverdraw() throws Exception {
    open("customer-myr", 1000, "user-1");
    open("nostro-myr", 0, null);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger ok = new AtomicInteger();
    AtomicInteger denied = new AtomicInteger();
    for (int i = 0; i < 2; i++) {
      String reference = "race-" + i;
      pool.submit(() -> {
        try {
          start.await();
          ledger.post(new JournalCommand("trace-race", reference, List.of(
              new Posting("customer-myr", EntrySide.DEBIT, new Money(1000, 2, "MYR")),
              new Posting("nostro-myr", EntrySide.CREDIT, new Money(1000, 2, "MYR")))));
          ok.incrementAndGet();
        } catch (BusinessException exception) {
          if (exception.code() == ErrorCode.INSUFFICIENT_FUNDS) {
            denied.incrementAndGet();
          } else {
            throw exception;
          }
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
      });
    }
    start.countDown();
    pool.shutdown();
    if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
      throw new IllegalStateException("race");
    }
    assertEquals(1, ok.get());
    assertEquals(1, denied.get());
    assertEquals(0L, available("customer-myr"));
  }

  /** 令牌里的用户不是账户归属人时不能扣该账户。 */
  @Test
  void foreignActorCannotDebitOwnedAccount() throws Exception {
    open("customer-myr", 2000, "user-1");
    open("nostro-myr", 0, null);
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("digitalunionpay")
        .subject("bob")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(600))
        .claim("uid", "user-2")
        .claim("grants", List.of("* /internal/**"))
        .build();
    String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    AccessContext.set(token);
    try {
      mvc.perform(post(LedgerPaths.JOURNALS).contentType(MediaType.APPLICATION_JSON).content(journal("ref-owner", 100)))
          .andExpect(status().isForbidden());
    } finally {
      AccessContext.clear();
    }
    assertEquals(2000L, available("customer-myr"));
  }

  /** 死信可以重新放回本地消息表。 */
  @Test
  void deadLetterCanBeRequeued() throws Exception {
    jdbc.update(
        "INSERT INTO dup_dead_letter (id, event_type, payload, reason, created_at) VALUES ('dead-1', 'journal.posted', 'ref-dead', 'attempts', CURRENT_TIMESTAMP)");
    mvc.perform(post("/internal/ops/dead-letters/dead-1/replay")).andExpect(status().isOk());
    assertEquals(0, count("SELECT COUNT(*) FROM dup_dead_letter"));
    assertEquals(1, count("SELECT COUNT(*) FROM dup_outbox WHERE idempotency_key = 'replay-dead-1'"));
  }

  /** 空账本上，负余额、不平分录和冻结偏差都是 0。 */
  @Test
  void ledgerAnomalyQueriesStayAtZero() {
    assertEquals(0, (int) LedgerWatch.negative(jdbc));
    assertEquals(0, (int) LedgerWatch.unbalanced(jdbc));
    assertEquals(0, (int) LedgerWatch.drift(jdbc));
  }

  /** 开一个有归属人的余额账户。 */
  private void open(String accountId, long available, String owner) {
    jdbc.update(
        "INSERT INTO ledger_account (id, account_id, currency, available, frozen, scale, version, owner_user_id) VALUES (?, ?, 'MYR', ?, 0, 2, 0, ?)",
        accountId.hashCode(),
        accountId,
        available,
        owner);
  }

  /** 一笔借客户、贷往来的分录。 */
  private static String journal(String reference, long minor) {
    return "{\"traceId\":\"trace-1\",\"reference\":\"" + reference + "\",\"postings\":["
        + "{\"accountId\":\"customer-myr\",\"side\":\"DEBIT\",\"amount\":{\"minorUnits\":" + minor + ",\"scale\":2,\"currency\":\"MYR\"}},"
        + "{\"accountId\":\"nostro-myr\",\"side\":\"CREDIT\",\"amount\":{\"minorUnits\":" + minor + ",\"scale\":2,\"currency\":\"MYR\"}}]}";
  }

  /** 读可用余额。 */
  private long available(String accountId) {
    return jdbc.queryForObject(
        "SELECT available FROM ledger_account WHERE account_id = ?", Long.class, accountId);
  }

  /** 读冻结余额。 */
  private long frozen(String accountId) {
    return jdbc.queryForObject(
        "SELECT frozen FROM ledger_account WHERE account_id = ?", Long.class, accountId);
  }

  /** 读一个计数。 */
  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }
}
