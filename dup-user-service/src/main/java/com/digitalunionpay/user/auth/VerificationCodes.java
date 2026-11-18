package com.digitalunionpay.user.auth;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.support.Outbox;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 短信和邮箱验证码。短信 5 分钟、每天 10 次；邮箱 10 分钟。明文只进本地消息，不返回给接口。 */
@Component
public class VerificationCodes {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final JdbcTemplate jdbc;

  private final Outbox outbox;

  private final Clock clock;

  private final int smsMinutes;

  private final int emailMinutes;

  private final int smsDailyLimit;

  /** 有效期和每日上限来自配置。 */
  public VerificationCodes(
      JdbcTemplate jdbc,
      Outbox outbox,
      Clock clock,
      @Value("${dup.auth.sms-minutes:5}") int smsMinutes,
      @Value("${dup.auth.email-minutes:10}") int emailMinutes,
      @Value("${dup.auth.sms-daily-limit:10}") int smsDailyLimit) {
    this.jdbc = jdbc;
    this.outbox = outbox;
    this.clock = clock;
    this.smsMinutes = smsMinutes;
    this.emailMinutes = emailMinutes;
    this.smsDailyLimit = smsDailyLimit;
  }

  /** 生成 6 位验证码并记下摘要。短信超过当日上限时拒绝。同一地址和用途只保留最新一张码。 */
  @Transactional
  public void send(String channel, String target, String purpose) {
    String kind = channel(channel);
    String address = Ids.required(target, "target");
    String why = purpose(purpose);
    if ("SMS".equals(kind)) {
      Instant start = LocalDateTime.now(clock).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
      Integer sent = jdbc.queryForObject(
          "SELECT COUNT(*) FROM verification_code WHERE channel = 'SMS' AND target = ? AND created_at >= ?",
          Integer.class,
          address,
          Timestamp.from(start));
      if (sent != null && sent >= smsDailyLimit) {
        throw new BusinessException(ErrorCode.RATE_LIMITED, "sms");
      }
    }
    jdbc.update(
        "UPDATE verification_code SET consumed_at = ? WHERE channel = ? AND target = ? AND purpose = ? AND consumed_at IS NULL",
        Timestamp.from(clock.instant()),
        kind,
        address,
        why);
    String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
    int minutes = "SMS".equals(kind) ? smsMinutes : emailMinutes;
    Instant expires = clock.instant().plus(minutes, ChronoUnit.MINUTES);
    jdbc.update(
        "INSERT INTO verification_code (id, channel, target, purpose, code_hash, attempts, expires_at, created_at, consumed_at) VALUES (?, ?, ?, ?, ?, 0, ?, ?, NULL)",
        LongIds.next(),
        kind,
        address,
        why,
        Digests.sha256(code),
        Timestamp.from(expires),
        Timestamp.from(clock.instant()));
    outbox.append("user.verification", kind + "|" + address + "|" + why + "|" + code, address + ":" + why + ":" + clock.millis());
  }

  /** 校验验证码。连续错误 5 次后这张码作废。 */
  @Transactional
  public void consume(String channel, String target, String purpose, String code) {
    String kind = channel(channel);
    String address = Ids.required(target, "target");
    String why = purpose(purpose);
    String presented = Ids.required(code, "code");
    if (!presented.matches("\\d{6}")) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "code");
    }
    Row row = jdbc.query(
        """
        SELECT id, code_hash, attempts, expires_at, consumed_at
        FROM verification_code
        WHERE channel = ? AND target = ? AND purpose = ? AND consumed_at IS NULL
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """,
        rs -> {
          if (!rs.next()) {
            return null;
          }
          return new Row(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getTimestamp(4).toInstant(), rs.getTimestamp(5) == null ? null : rs.getTimestamp(5).toInstant());
        },
        kind,
        address,
        why);
    if (row == null || row.consumedAt != null || clock.instant().isAfter(row.expiresAt)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "code");
    }
    if (row.attempts >= 5 || !Digests.sha256(presented).equals(row.codeHash)) {
      jdbc.update(
          "UPDATE verification_code SET attempts = attempts + 1, consumed_at = CASE WHEN attempts + 1 >= 5 THEN ? ELSE consumed_at END WHERE id = ?",
          Timestamp.from(clock.instant()),
          row.id);
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "code");
    }
    jdbc.update("UPDATE verification_code SET consumed_at = ? WHERE id = ?", Timestamp.from(clock.instant()), row.id);
  }

  /** 只接受短信和邮箱。 */
  public static String channel(String channel) {
    String kind = Ids.required(channel, "channel").trim().toUpperCase();
    if (!"SMS".equals(kind) && !"EMAIL".equals(kind)) {
      throw new IllegalArgumentException("channel");
    }
    return kind;
  }

  /** 注册、登录和绑定三类用途。 */
  public static String purpose(String purpose) {
    String why = Ids.required(purpose, "purpose").trim().toUpperCase();
    if (!"REGISTER".equals(why) && !"LOGIN".equals(why) && !"BIND".equals(why)) {
      throw new IllegalArgumentException("purpose");
    }
    return why;
  }

  /** 最新一张验证码。 */
  private record Row(long id, String codeHash, int attempts, Instant expiresAt, Instant consumedAt) {
  }
}
