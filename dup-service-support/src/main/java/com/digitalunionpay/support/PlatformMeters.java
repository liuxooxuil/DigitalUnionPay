package com.digitalunionpay.support;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

/** 账本、消息和慢调用的计数。没有注册表时调用被忽略。 */
public final class PlatformMeters {

  /** 未发送的本地消息条数。 */
  public static final String OUTBOX_BACKLOG = "dup.outbox.backlog";

  /** 余额不足。 */
  public static final String LEDGER_INSUFFICIENT = "dup.ledger.insufficient";

  /** 同一业务单号再次入账。 */
  public static final String LEDGER_DUPLICATE = "dup.ledger.duplicate";

  /** 单次数据库调用超过阈值。 */
  public static final String DB_SLOW = "dup.db.slow";

  private final MeterRegistry registry;

  /** registry 为空时只保留调用点，不报错。 */
  public PlatformMeters(MeterRegistry registry) {
    this.registry = registry;
  }

  /** 计数加一。 */
  public void mark(String name) {
    if (registry == null || name == null || name.isBlank()) {
      return;
    }
    registry.counter(name).increment();
  }

  /** 超过阈值就记一次慢调用。 */
  public void slow(long elapsedMillis, long thresholdMillis) {
    if (elapsedMillis >= thresholdMillis) {
      mark(DB_SLOW);
    }
  }

  /** 给积压仪表读未发送行数。查询失败时记 0，避免把采集打挂。 */
  public static double backlog(JdbcTemplate jdbc) {
    try {
      Integer count = jdbc.queryForObject(
          "SELECT COUNT(*) FROM dup_outbox WHERE sent_at IS NULL", Integer.class);
      return count == null ? 0 : count;
    } catch (RuntimeException exception) {
      return 0;
    }
  }
}
