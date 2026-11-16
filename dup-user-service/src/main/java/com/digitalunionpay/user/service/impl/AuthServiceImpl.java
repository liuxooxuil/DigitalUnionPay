package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.AuthPort;
import com.digitalunionpay.user.IssuedToken;
import com.digitalunionpay.user.LoginCommand;
import com.digitalunionpay.support.AccessGrants;
import com.digitalunionpay.support.AccessJwt;
import com.digitalunionpay.user.entity.AuthTokenEntity;
import com.digitalunionpay.user.entity.RbacGrant;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.mapper.AuthTokenMapper;
import com.digitalunionpay.user.mapper.RbacGrantMapper;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/** 登录签发 token，并按角色权限判断能否访问接口。 */
@Service
public final class AuthServiceImpl implements AuthPort {

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final AntPathMatcher PATHS = new AntPathMatcher();

  private final RbacUserMapper users;

  private final AuthTokenMapper tokens;

  private final RbacGrantMapper grants;

  private final PasswordEncoder passwords;

  private final JwtEncoder encoder;

  private final JwtDecoder decoder;

  /** 接入账号、token、权限、密码摘要和 JWT。 */
  public AuthServiceImpl(
      RbacUserMapper users,
      AuthTokenMapper tokens,
      RbacGrantMapper grants,
      PasswordEncoder passwords,
      JwtEncoder encoder,
      JwtDecoder decoder) {
    this.users = users;
    this.tokens = tokens;
    this.grants = grants;
    if (passwords == null || encoder == null || decoder == null) {
      throw new IllegalArgumentException("auth");
    }
    this.passwords = passwords;
    this.encoder = encoder;
    this.decoder = decoder;
  }

  /** 账号不存在、密码错误或已停用时拒绝登录。 */
  @Override
  public IssuedToken login(LoginCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    Ids.required(command.username(), "username");
    Ids.required(command.password(), "password");
    RbacUserEntity user = users.findByUsername(command.username());
    if (user == null || !passwords.matches(command.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "login");
    }
    if (!"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "user");
    }
    List<String> grantTexts = new ArrayList<>();
    for (RbacGrant grant : grants.findByUserId(user.getUserId())) {
      grantTexts.add(AccessGrants.format(grant.getHttpMethod(), grant.getPathPattern()));
    }
    String jti = newToken();
    Instant expiresAt = Instant.now().plus(12, ChronoUnit.HOURS);
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(AccessJwt.ISSUER)
        .subject(user.getUsername())
        .id(jti)
        .issuedAt(Instant.now())
        .expiresAt(expiresAt)
        .claim("uid", user.getUserId())
        .claim("grants", grantTexts)
        .build();
    String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    AuthTokenEntity row = new AuthTokenEntity();
    row.setTokenHash(sha256(jti));
    row.setUserId(user.getUserId());
    row.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    tokens.insert(row);
    return new IssuedToken(token, user.getUserId(), row.getExpiresAt());
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
    AuthTokenEntity row = tokens.findByHash(sha256(jwt.getId()));
    if (row == null || row.getExpiresAt() == null || row.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
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

  /** 32 字节随机数的十六进制，作为明文 token。 */
  private static String newToken() {
    byte[] raw = new byte[32];
    RANDOM.nextBytes(raw);
    return HexFormat.of().formatHex(raw);
  }

  /** token 只存 SHA-256，库里看不到明文。 */
  private static String sha256(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("token");
    }
  }
}
