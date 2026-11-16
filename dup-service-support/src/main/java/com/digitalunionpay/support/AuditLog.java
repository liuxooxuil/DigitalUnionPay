package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;

/** 资金和权限动作的审计行。和业务写在同一个事务里时，业务回滚审计也会回滚。 */
public final class AuditLog {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 保存时钟。 */
  public AuditLog(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** 记下动作、主体和短说明。链路号来自当前 MDC。 */
  public void record(String action, String subject, String detail) {
    Ids.required(action, "action");
    jdbc.update(
        "INSERT INTO dup_audit (id, action, subject, detail, trace_id, created_at) VALUES (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID().toString(),
        action,
        subject == null ? "" : subject,
        detail == null ? "" : detail,
        trace(MDC.get(TraceIdFilter.HEADER)),
        Timestamp.from(clock.instant()));
  }

  /** 没有链路号时写空字符串，避免审计表出现空值。 */
  private static String trace(String value) {
    return value == null ? "" : value;
  }
}
