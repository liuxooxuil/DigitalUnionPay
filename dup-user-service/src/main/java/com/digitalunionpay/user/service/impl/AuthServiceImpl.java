package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.AuthPort;
import com.digitalunionpay.user.IssuedToken;
import com.digitalunionpay.user.LoginCommand;
import com.digitalunionpay.user.auth.SessionIssuer;
import com.digitalunionpay.user.auth.VerificationCodes;
import com.digitalunionpay.user.entity.AuthTokenEntity;
import com.digitalunionpay.user.entity.RbacGrant;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.mapper.AuthTokenMapper;
import com.digitalunionpay.user.mapper.RbacGrantMapper;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/** 登录签发 token，并按角色权限判断能否访问接口。 */
@Service
public final class AuthServiceImpl implements AuthPort {

  private static final AntPathMatcher PATHS = new AntPathMatcher();

  private final RbacUserMapper users;

  private final AuthTokenMapper tokens;

  private final RbacGrantMapper grants;

  private final PasswordEncoder passwords;

  private final JwtDecoder decoder;

  private final SessionIssuer sessions;

  private final VerificationCodes codes;

  private final JdbcTemplate jdbc;

  private final Clock clock;

  private final int lockFailures;

  private final int lockMinutes;

  /** 接入账号、会话、验证码和锁定阈值。 */
  public AuthServiceImpl(
      RbacUserMapper users,
      AuthTokenMapper tokens,
      RbacGrantMapper grants,
      PasswordEncoder passwords,
      JwtDecoder decoder,
      SessionIssuer sessions,
      VerificationCodes codes,
      JdbcTemplate jdbc,
      Clock clock,
      @Value("${dup.auth.lock-failures:5}") int lockFailures,
      @Value("${dup.auth.lock-minutes:30}") int lockMinutes) {
    this.users = users;
    this.tokens = tokens;
    this.grants = grants;
    if (passwords == null || decoder == null) {
      throw new IllegalArgumentException("auth");
    }
    this.passwords = passwords;
    this.decoder = decoder;
    this.sessions = sessions;
    this.codes = codes;
    this.jdbc = jdbc;
    this.clock = clock;
    this.lockFailures = lockFailures;
    this.lockMinutes = lockMinutes;
  }

  /** 连续失败达到阈值后锁定。二次验证打开时，密码正确还要再对一次验证码。 */
  @Override
  public IssuedToken login(LoginCommand command, String clientIp) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    Ids.required(command.username(), "username");
    Ids.required(command.password(), "password");
    RbacUserEntity user = users.findByUsername(command.username());
    if (user == null || !passwords.matches(command.password(), user.getPasswordHash())) {
      if (user != null && fail(user.getUserId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "locked");
      }
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "login");
    }
    assertUnlocked(user.getUserId());
    if (mfa(user.getUserId())) {
      if (command.code() == null || command.code().isBlank()) {
        codes.send(channelOf(user), user.getUsername(), "LOGIN");
        throw new BusinessException(ErrorCode.UNAUTHENTICATED, "mfa");
      }
      codes.consume(channelOf(user), user.getUsername(), "LOGIN", command.code());
    }
    clearFailures(user.getUserId());
    return sessions.issue(user, command.deviceId(), command.deviceName(), clientIp);
  }

  /** 过期、停用或没有匹配的方法与路径时拒绝。这里按库里的权限复查，不单信 JWT 声明。 */
  @Override
  public void check(String token, String method, String path) {
    Ids.required(token, "token");
    Ids.required(method, "method");
    Ids.required(path, "path");
    Jwt jwt;
    try {
      jwt = decoder.decode(token);
    } catch (JwtException exception) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    if (jwt.getId() == null || jwt.getId().isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    AuthTokenEntity row = tokens.findByHash(SessionIssuer.sha256(jwt.getId()));
    sessions.touch(row);
    RbacUserEntity user = users.findByUserId(row.getUserId());
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "user");
    }
    for (RbacGrant grant : grants.findByUserId(user.getUserId())) {
      if (methodMatches(grant.getHttpMethod(), method) && PATHS.match(grant.getPathPattern(), path)) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, path);
  }

  /** * 表示不限方法。 */
  private static boolean methodMatches(String granted, String actual) {
    return "*".equals(granted) || (granted != null && granted.equalsIgnoreCase(actual));
  }

  /** 锁定未到期时拒绝。到期后清掉连续失败次数。 */
  private void assertUnlocked(String userId) {
    LocalDateTime locked = jdbc.query(
        "SELECT locked_until FROM rbac_user WHERE user_id = ?",
        rs -> {
          if (!rs.next() || rs.getTimestamp(1) == null) {
            return null;
          }
          return LocalDateTime.ofInstant(rs.getTimestamp(1).toInstant(), ZoneOffset.UTC);
        },
        userId);
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    if (locked != null && locked.isAfter(now)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "locked");
    }
    if (locked != null) {
      clearFailures(userId);
    }
  }

  /** 密码错误累加。达到阈值时锁定并返回 true。 */
  private boolean fail(String userId) {
    assertUnlocked(userId);
    Integer current = jdbc.queryForObject("SELECT failed_logins FROM rbac_user WHERE user_id = ?", Integer.class, userId);
    int next = (current == null ? 0 : current) + 1;
    LocalDateTime until = null;
    if (next >= lockFailures) {
      next = lockFailures;
      until = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusMinutes(lockMinutes);
    }
    jdbc.update(
        "UPDATE rbac_user SET failed_logins = ?, locked_until = ? WHERE user_id = ?",
        next,
        until == null ? null : Timestamp.from(until.toInstant(ZoneOffset.UTC)),
        userId);
    return until != null;
  }

  /** 登录成功后连续失败次数归零。 */
  private void clearFailures(String userId) {
    jdbc.update("UPDATE rbac_user SET failed_logins = 0, locked_until = NULL WHERE user_id = ?", userId);
  }

  /** 是否要求二次验证。 */
  private boolean mfa(String userId) {
    Integer enabled = jdbc.queryForObject("SELECT mfa_enabled FROM rbac_user WHERE user_id = ?", Integer.class, userId);
    return enabled != null && enabled == 1;
  }

  /** 手机号登录名走短信，其余走邮箱。 */
  private static String channelOf(RbacUserEntity user) {
    return user.getUsername() != null && user.getUsername().startsWith("+") ? "SMS" : "EMAIL";
  }
}
