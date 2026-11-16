package com.digitalunionpay.ledger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 账本异常的只读计数：负余额、借贷不平、冻结与在途冻结不一致。采集失败时记 0。 */
@Component
public class LedgerWatch {

  /** 注册三个仪表。抓取时才查询。 */
  public LedgerWatch(ObjectProvider<MeterRegistry> registries, JdbcTemplate jdbc) {
    MeterRegistry registry = registries.getIfAvailable();
    if (registry == null) {
      return;
    }
    Gauge.builder("dup.ledger.negative", jdbc, LedgerWatch::safeNegative).register(registry);
    Gauge.builder("dup.ledger.unbalanced", jdbc, LedgerWatch::safeUnbalanced).register(registry);
    Gauge.builder("dup.ledger.freeze_drift", jdbc, LedgerWatch::safeDrift).register(registry);
  }

  /** 可用或冻结小于 0 的账户数。 */
  static double negative(JdbcTemplate jdbc) {
    return query(jdbc, "SELECT COUNT(*) FROM ledger_account WHERE available < 0 OR frozen < 0");
  }

  /** 借贷合计不相等的分录数。 */
  static double unbalanced(JdbcTemplate jdbc) {
    return query(jdbc, """
        SELECT COUNT(*) FROM (
          SELECT j.id FROM ledger_journal j
          LEFT JOIN ledger_posting p ON p.journal_id = j.id
          GROUP BY j.id
          HAVING COALESCE(SUM(CASE WHEN p.side = 'DEBIT' THEN p.minor_units ELSE 0 END), 0)
              <> COALESCE(SUM(CASE WHEN p.side = 'CREDIT' THEN p.minor_units ELSE 0 END), 0)
        ) unbalanced
        """);
  }

  /** 账户冻结额与状态为 TRIED 的冻结合计不一致的账户数。 */
  static double drift(JdbcTemplate jdbc) {
    return query(jdbc, """
        SELECT COUNT(*) FROM ledger_account a
        WHERE a.frozen <> COALESCE((
          SELECT SUM(f.minor_units) FROM ledger_freeze f
          WHERE f.account_id = a.account_id AND f.currency = a.currency AND f.status = 'TRIED'
        ), 0)
        """);
  }

  private static double safeNegative(JdbcTemplate jdbc) {
    return safe(() -> negative(jdbc));
  }

  private static double safeUnbalanced(JdbcTemplate jdbc) {
    return safe(() -> unbalanced(jdbc));
  }

  private static double safeDrift(JdbcTemplate jdbc) {
    return safe(() -> drift(jdbc));
  }

  private static double safe(java.util.function.DoubleSupplier query) {
    try {
      return query.getAsDouble();
    } catch (RuntimeException exception) {
      return 0;
    }
  }

  private static double query(JdbcTemplate jdbc, String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }
}
