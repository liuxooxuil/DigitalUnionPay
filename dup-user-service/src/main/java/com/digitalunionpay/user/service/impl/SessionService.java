package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.IssuedToken;
import com.digitalunionpay.user.auth.Contacts;
import com.digitalunionpay.user.auth.LongIds;
import com.digitalunionpay.user.auth.SessionIssuer;
import com.digitalunionpay.user.auth.VerificationCodes;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import com.digitalunionpay.user.service.CustomerForms;
import com.digitalunionpay.user.service.CustomerViews;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 验证码登录、生物识别和在线设备。类不是 final，事务代理要生成子类。 */
@Service
public class SessionService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final RbacUserMapper users;

  private final VerificationCodes codes;

  private final SessionIssuer sessions;

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 接入账号、验证码和会话签发。 */
  public SessionService(
      RbacUserMapper users, VerificationCodes codes, SessionIssuer sessions, JdbcTemplate jdbc, Clock clock) {
    this.users = users;
    this.codes = codes;
    this.sessions = sessions;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * 发送验证码。登录用途在账号不存在时仍返回成功，但不落库，避免被人探出哪些号码注册过。
   * 绑定用途必须已经登录。
   */
  @Transactional
  public void send(CustomerForms.SendCode form, String userId) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String channel = VerificationCodes.channel(form.channel());
    String purpose = VerificationCodes.purpose(form.purpose());
    String target = Contacts.target(channel, form.countryCode(), form.phone(), form.email());
    if ("BIND".equals(purpose)) {
      if (userId == null || userId.isBlank()) {
        throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
      }
    }
    if ("LOGIN".equals(purpose) && users.findByUsername(target) == null) {
      return;
    }
    codes.send(channel, target, purpose);
  }

  /** 验证码正确就签发会话。失败不计入密码锁定。 */
  @Transactional
  public IssuedToken loginWithCode(CustomerForms.CodeLogin form, String clientIp) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String channel = VerificationCodes.channel(form.channel());
    String username = Contacts.target(channel, form.countryCode(), form.phone(), form.email());
    RbacUserEntity user = users.findByUsername(username);
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "code");
    }
    codes.consume(channel, username, "LOGIN", form.code());
    return sessions.issue(user, form.deviceId(), form.deviceName(), clientIp);
  }

  /** 绑定当前用户这台设备的 Ed25519 公钥。公钥必须是 X.509 编码。 */
  @Transactional
  public void bind(String userId, CustomerForms.BindBiometric form) {
    Ids.required(userId, "userId");
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String device = device(form.deviceId());
    parseKey(form.publicKey());
    int updated = jdbc.update(
        "UPDATE device_credential SET public_key = ? WHERE user_id = ? AND device_id = ?",
        form.publicKey().trim(),
        userId,
        device);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO device_credential (id, user_id, device_id, public_key, created_at) VALUES (?, ?, ?, ?, ?)",
          LongIds.next(),
          userId,
          device,
          form.publicKey().trim(),
          Timestamp.from(clock.instant()));
    }
  }

  /** 签发两分钟有效的一次性挑战。不要求已经登录。 */
  @Transactional
  public String challenge(CustomerForms.BiometricChallenge form) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String device = device(form.deviceId());
    byte[] raw = new byte[32];
    RANDOM.nextBytes(raw);
    String challenge = HexFormat.of().formatHex(raw);
    jdbc.update(
        "INSERT INTO biometric_challenge (id, device_id, challenge, expires_at, consumed_at) VALUES (?, ?, ?, ?, NULL)",
        LongIds.next(),
        device,
        challenge,
        Timestamp.from(clock.instant().plus(2, ChronoUnit.MINUTES)));
    return challenge;
  }

  /** 用挑战的 Ed25519 签名换会话。挑战只用一次。 */
  @Transactional
  public IssuedToken loginWithBiometric(CustomerForms.BiometricLogin form, String clientIp) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String device = device(form.deviceId());
    String challenge = Ids.required(form.challenge(), "challenge");
    String signature = Ids.required(form.signature(), "signature");
    Row stored = jdbc.query(
        """
        SELECT id, expires_at, consumed_at FROM biometric_challenge
        WHERE device_id = ? AND challenge = ?
        """,
        rs -> {
          if (!rs.next()) {
            return null;
          }
          return new Row(rs.getLong(1), rs.getTimestamp(2).toInstant(), rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant());
        },
        device,
        challenge);
    if (stored == null || stored.consumedAt != null || clock.instant().isAfter(stored.expiresAt)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "challenge");
    }
    List<Credential> credentials = jdbc.query(
        "SELECT user_id, public_key FROM device_credential WHERE device_id = ?",
        (rs, row) -> new Credential(rs.getString(1), rs.getString(2)),
        device);
    if (credentials.size() != 1 || !verify(credentials.get(0).publicKey, challenge, signature)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "biometric");
    }
    jdbc.update(
        "UPDATE biometric_challenge SET consumed_at = ? WHERE id = ?",
        Timestamp.from(clock.instant()),
        stored.id);
    RbacUserEntity user = users.findByUserId(credentials.get(0).userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "biometric");
    }
    return sessions.issue(user, device, "", clientIp);
  }

  /** 仍有效、未撤销、也没有空闲超时的设备。 */
  public List<CustomerViews.SessionDevice> devices(String userId, long currentSession) {
    Ids.required(userId, "userId");
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    LocalDateTime idle = now.minusHours(sessions.idleHours());
    return jdbc.query(
        """
        SELECT id, device_id, device_name, client_ip, location_key, last_seen_at
        FROM auth_token
        WHERE user_id = ? AND revoked_at IS NULL AND expires_at > ?
          AND (last_seen_at IS NULL OR last_seen_at > ?)
        ORDER BY id
        """,
        (rs, row) -> {
          LocalDateTime seen = rs.getObject(6, LocalDateTime.class);
          return new CustomerViews.SessionDevice(
              rs.getLong(1),
              text(rs.getString(2)),
              text(rs.getString(3)),
              text(rs.getString(4)),
              text(rs.getString(5)),
              seen == null ? "" : seen.toString(),
              rs.getLong(1) == currentSession);
        },
        userId,
        now,
        idle);
  }

  /** 远程下线。只能下线自己的会话。 */
  @Transactional
  public void revoke(String userId, long sessionId) {
    Ids.required(userId, "userId");
    int updated = jdbc.update(
        "UPDATE auth_token SET revoked_at = ? WHERE id = ? AND user_id = ? AND revoked_at IS NULL",
        Timestamp.from(clock.instant()),
        sessionId,
        userId);
    if (updated == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "session");
    }
  }

  /** 设备号沿用会话签发的长度限制。 */
  private static String device(String deviceId) {
    String device = Ids.required(deviceId, "deviceId").trim();
    if (device.length() > 64) {
      throw new IllegalArgumentException("deviceId");
    }
    return device;
  }

  /** 解析失败说明不是 Ed25519 的 X.509 公钥。 */
  private static PublicKey parseKey(String publicKey) {
    String text = Ids.required(publicKey, "publicKey").trim();
    try {
      byte[] encoded = Base64.getDecoder().decode(text);
      return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalArgumentException("publicKey");
    }
  }

  /** 签名覆盖挑战原文的 UTF-8 字节。 */
  private static boolean verify(String publicKey, String challenge, String signature) {
    try {
      PublicKey key = parseKey(publicKey);
      Signature verifier = Signature.getInstance("Ed25519");
      verifier.initVerify(key);
      verifier.update(challenge.getBytes(StandardCharsets.UTF_8));
      return verifier.verify(Base64.getDecoder().decode(signature.trim()));
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      return false;
    }
  }

  /** 空白字段返回空串，避免响应里出现 null。 */
  private static String text(String value) {
    return value == null ? "" : value;
  }

  /** 库里的一条挑战。 */
  private record Row(long id, java.time.Instant expiresAt, java.time.Instant consumedAt) {
  }

  /** 一台设备只应绑在一个用户上。 */
  private record Credential(String userId, String publicKey) {
  }
}
