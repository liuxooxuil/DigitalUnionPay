package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** 应用侧死信表。投递多次仍失败的消息停在这里，不再重试。 */
public final class DeadLetters {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 保存写入时间用的时钟。 */
  public DeadLetters(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** 记下事件类型、报文体和失败原因。 */
  public void park(String type, String payload, String reason) {
    Ids.required(type, "type");
    Ids.required(reason, "reason");
    jdbc.update(
        "INSERT INTO dup_dead_letter (id, event_type, payload, reason, created_at) VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID().toString(),
        type,
        payload == null ? "" : payload,
        reason,
        Timestamp.from(clock.instant()));
  }
}
