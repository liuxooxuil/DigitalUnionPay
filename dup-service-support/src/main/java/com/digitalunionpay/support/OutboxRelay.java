package com.digitalunionpay.support;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/** 事务提交后把本地消息表投到 RocketMQ。连续失败五次后转入死信表。 */
public final class OutboxRelay {

  private static final int ATTEMPTS = 5;

  private final JdbcTemplate jdbc;

  private final DomainEvents events;

  private final DeadLetters deadLetters;

  private final Clock clock;

  private final PlatformMeters meters;

  /** 接入消息表、发送器、死信表和慢调用计数。 */
  public OutboxRelay(
      JdbcTemplate jdbc, DomainEvents events, DeadLetters deadLetters, Clock clock, PlatformMeters meters) {
    if (jdbc == null || events == null || deadLetters == null) {
      throw new IllegalArgumentException("relay");
    }
    this.jdbc = jdbc;
    this.events = events;
    this.deadLetters = deadLetters;
    this.clock = clock == null ? Clock.systemUTC() : clock;
    this.meters = meters;
  }

  /** 每次最多投递 50 条未发送记录。单条失败不影响后面的记录。查询超过 200 毫秒记一次慢调用。 */
  @Scheduled(fixedDelay = 2000)
  public void publishPending() {
    long started = System.nanoTime();
    List<Row> rows = jdbc.query(
        "SELECT id, event_type, payload, idempotency_key, attempts FROM dup_outbox WHERE sent_at IS NULL ORDER BY created_at LIMIT 50",
        (rs, row) -> new Row(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)));
    if (meters != null) {
      meters.slow((System.nanoTime() - started) / 1_000_000L, 200L);
    }
    for (Row pending : rows) {
      deliver(pending);
    }
  }

  /** 发送成功就标出站时间。失败累加次数，满五次写入死信并停止重试。 */
  private void deliver(Row pending) {
    try {
      events.publish(pending.type, pending.payload, pending.key);
      jdbc.update(
          "UPDATE dup_outbox SET sent_at = ? WHERE id = ? AND sent_at IS NULL",
          Timestamp.from(clock.instant()),
          pending.id);
    } catch (RuntimeException exception) {
      int attempts = pending.attempts + 1;
      if (attempts >= ATTEMPTS) {
        deadLetters.park(pending.type, pending.payload, "attempts");
        jdbc.update(
            "UPDATE dup_outbox SET attempts = ?, sent_at = ? WHERE id = ?",
            attempts,
            Timestamp.from(clock.instant()),
            pending.id);
        return;
      }
      jdbc.update("UPDATE dup_outbox SET attempts = ? WHERE id = ?", attempts, pending.id);
    }
  }

  /** 一次待投递的本地消息。 */
  private record Row(String id, String type, String payload, String key, int attempts) {
  }
}
