package com.digitalunionpay.support;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** 记住已经用过的服务调用 nonce。重复的视为重放。 */
public final class NonceStore {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 保存时钟，并清掉一天前的 nonce。 */
  public NonceStore(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** 第一次见到返回 true。主键冲突返回 false。 */
  public boolean remember(String nonce) {
    jdbc.update(
        "DELETE FROM dup_service_nonce WHERE seen_at < ?",
        Timestamp.from(clock.instant().minus(Duration.ofDays(1))));
    try {
      jdbc.update(
          "INSERT INTO dup_service_nonce (nonce, seen_at) VALUES (?, ?)",
          nonce,
          Timestamp.from(clock.instant()));
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }
}
