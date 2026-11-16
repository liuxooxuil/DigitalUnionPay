package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** 用一张主键表记住已经受理的幂等键。每个业务进程启动时补齐这张表。 */
final class JdbcIdempotencyStore implements IdempotencyStore {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 建表并保存后续插入要用的时钟。 */
  JdbcIdempotencyStore(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
    jdbc.execute("""
        CREATE TABLE IF NOT EXISTS dup_idempotency (
          scope VARCHAR(64) NOT NULL,
          idem_key VARCHAR(128) NOT NULL,
          created_at DATETIME NOT NULL,
          PRIMARY KEY (scope, idem_key)
        )
        """);
  }

  /** 空白键直接拒绝。主键冲突视为重复提交。 */
  @Override
  public boolean claim(String scope, String key) {
    Ids.required(scope, "scope");
    Ids.required(key, "key");
    try {
      jdbc.update(
          "INSERT INTO dup_idempotency (scope, idem_key, created_at) VALUES (?, ?, ?)",
          scope,
          key,
          Timestamp.from(clock.instant()));
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }
}
