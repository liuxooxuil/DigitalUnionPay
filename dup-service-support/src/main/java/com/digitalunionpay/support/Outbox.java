package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** 本地消息表。要和业务写入放在同一个事务里，提交之后再投递。 */
public final class Outbox {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 保存后续插入要用的时钟。 */
  public Outbox(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** 只落库，不在这里发送。发送失败不会把业务事务一起回滚。 */
  public void append(String type, String payload, String idempotencyKey) {
    Ids.required(type, "type");
    Ids.required(payload, "payload");
    Ids.required(idempotencyKey, "idempotencyKey");
    jdbc.update(
        "INSERT INTO dup_outbox (id, event_type, payload, idempotency_key, attempts, created_at, sent_at) VALUES (?, ?, ?, ?, 0, ?, NULL)",
        UUID.randomUUID().toString(),
        type,
        payload,
        idempotencyKey,
        Timestamp.from(clock.instant()));
  }
}
