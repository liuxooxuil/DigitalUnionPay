package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** 消费侧幂等。同一个事件键第二次出现时直接跳过。 */
public final class Inbox {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 保存时钟。 */
  public Inbox(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** 第一次见到返回 true。主键冲突返回 false。 */
  public boolean first(String eventId, String type, int version) {
    Ids.required(eventId, "eventId");
    Ids.required(type, "type");
    try {
      jdbc.update(
          "INSERT INTO dup_inbox (event_id, event_type, event_version, consumed_at) VALUES (?, ?, ?, ?)",
          eventId,
          type,
          version,
          Timestamp.from(clock.instant()));
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }
}
