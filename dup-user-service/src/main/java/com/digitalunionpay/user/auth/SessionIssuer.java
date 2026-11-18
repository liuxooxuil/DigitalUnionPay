package com.digitalunionpay.user.auth;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.support.AccessGrants;
import com.digitalunionpay.support.AccessJwt;
import com.digitalunionpay.support.Outbox;
import com.digitalunionpay.user.IssuedToken;
import com.digitalunionpay.user.entity.AuthTokenEntity;
import com.digitalunionpay.user.entity.RbacGrant;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.mapper.AuthTokenMapper;
import com.digitalunionpay.user.mapper.RbacGrantMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 签发会话。默认 2 小时无操作过期，绝对有效期 12 小时，最多 5 台设备。 */
@Component
public class SessionIssuer {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final AuthTokenMapper tokens;

  private final RbacGrantMapper grants;

  private final JdbcTemplate jdbc;

  private final JwtEncoder encoder;

  private final Outbox outbox;

  private final Clock clock;

  private final int idleHours;

  private final int sessionHours;

  private final int maxDevices;

  /** 接入 token 表、权限和有效期配置。 */
  public SessionIssuer(
      AuthTokenMapper tokens,
      RbacGrantMapper grants,
      JdbcTemplate jdbc,
      JwtEncoder encoder,
      Outbox outbox,
      Clock clock,
      @Value("${dup.auth.idle-hours:2}") int idleHours,
      @Value("${dup.auth.session-hours:12}") int sessionHours,
      @Value("${dup.auth.max-devices:5}") int maxDevices) {
    this.tokens = tokens;
    this.grants = grants;
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.outbox = outbox;
    this.clock = clock;
    this.idleHours = idleHours;
    this.sessionHours = sessionHours;
    this.maxDevices = maxDevices;
  }

  /** 同一设备再次登录会替换旧会话。超过设备上限时拒绝。新设备或异地登录写告警消息。 */
  @Transactional
  public IssuedToken issue(RbacUserEntity user, String deviceId, String deviceName, String clientIp) {
    if (!"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "user");
    }
    String ip = Ids.required(clientIp, "ip");
    String device = deviceId == null || deviceId.isBlank() ? "ip:" + ip : deviceId.trim();
    if (device.length() > 64) {
      throw new IllegalArgumentException("deviceId");
    }
    String location = location(ip);
    boolean seen = count(
        "SELECT COUNT(*) FROM auth_token WHERE user_id = ? AND device_id = ?",
        user.getUserId(),
        device) > 0;
    String previous = jdbc.query(
        "SELECT location_key FROM auth_token WHERE user_id = ? ORDER BY id DESC LIMIT 1",
        rs -> rs.next() ? rs.getString(1) : null,
        user.getUserId());
    jdbc.update(
        "UPDATE auth_token SET revoked_at = ? WHERE user_id = ? AND device_id = ? AND revoked_at IS NULL",
        Timestamp.from(clock.instant()),
        user.getUserId(),
        device);
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    int active = count(
        """
        SELECT COUNT(*) FROM auth_token
        WHERE user_id = ? AND revoked_at IS NULL AND expires_at > ?
          AND (last_seen_at IS NULL OR last_seen_at > ?)
        """,
        user.getUserId(),
        now,
        now.minusHours(idleHours));
    if (active >= maxDevices) {
      throw new BusinessException(ErrorCode.CONFLICT, "device");
    }
    List<String> grantTexts = new ArrayList<>();
    for (RbacGrant grant : grants.findByUserId(user.getUserId())) {
      grantTexts.add(AccessGrants.format(grant.getHttpMethod(), grant.getPathPattern()));
    }
    String jti = newToken();
    Instant expiresAt = clock.instant().plus(sessionHours, ChronoUnit.HOURS);
    String token = encoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(),
        JwtClaimsSet.builder()
            .issuer(AccessJwt.ISSUER)
            .subject(user.getUsername())
            .id(jti)
            .issuedAt(clock.instant())
            .expiresAt(expiresAt)
            .claim("uid", user.getUserId())
            .claim("grants", grantTexts)
            .build())).getTokenValue();
    AuthTokenEntity row = new AuthTokenEntity();
    row.setTokenHash(sha256(jti));
    row.setUserId(user.getUserId());
    row.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    row.setDeviceId(device);
    row.setDeviceName(deviceName == null ? "" : deviceName.trim());
    row.setClientIp(ip);
    row.setLocationKey(location);
    row.setLastSeenAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    tokens.insert(row);
    if (!seen) {
      alert(user.getUserId(), device, ip, "NEW_DEVICE");
    } else if (previous != null && !previous.equals(location)) {
      alert(user.getUserId(), device, ip, "REMOTE");
    }
    return new IssuedToken(token, user.getUserId(), row.getExpiresAt(), row.getId());
  }

  /** 撤销、绝对过期或超过空闲时间都拒绝，并滑动最近活动时间。 */
  @Transactional
  public void touch(AuthTokenEntity row) {
    if (row == null || row.getExpiresAt() == null) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    if (row.getRevokedAt() != null || row.getExpiresAt().isBefore(now)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    if (row.getLastSeenAt() != null && row.getLastSeenAt().plusHours(idleHours).isBefore(now)) {
      jdbc.update("UPDATE auth_token SET revoked_at = ? WHERE id = ?", Timestamp.from(clock.instant()), row.getId());
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "idle");
    }
    jdbc.update(
        "UPDATE auth_token SET last_seen_at = ? WHERE id = ?",
        LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
        row.getId());
  }

  /** 当前空闲小时数，供会话查询显示。 */
  public int idleHours() {
    return idleHours;
  }

  /** IPv4 取前两段作为异地判断。其他地址整段比较。 */
  static String location(String ip) {
    String[] parts = ip.split("\\.");
    if (parts.length == 4) {
      return parts[0] + "." + parts[1];
    }
    return ip;
  }

  /** 告警进本地消息，由通知链路再发短信或邮件。 */
  private void alert(String userId, String device, String ip, String reason) {
    outbox.append("user.login.alert", userId + "|" + reason + "|" + device + "|" + ip, userId + ":" + reason + ":" + clock.millis());
  }

  /** 计数查询。 */
  private int count(String sql, Object... args) {
    Integer count = jdbc.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  /** 32 字节随机数的十六进制，作为 JWT 的 jti。 */
  private static String newToken() {
    byte[] raw = new byte[32];
    RANDOM.nextBytes(raw);
    return HexFormat.of().formatHex(raw);
  }

  /** jti 只存 SHA-256。登录校验与当前用户解析都要按同一算法比对。 */
  public static String sha256(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("token");
    }
  }
}
