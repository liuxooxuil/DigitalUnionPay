package com.digitalunionpay.user.auth;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.support.AccessTokens;
import com.digitalunionpay.user.entity.AuthTokenEntity;
import com.digitalunionpay.user.mapper.AuthTokenMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/** 从 Authorization 取出当前用户，并顺带检查会话是否空闲超时。 */
@Component
public class CurrentUser {

  private final JwtDecoder decoder;

  private final AuthTokenMapper tokens;

  private final SessionIssuer sessions;

  /** 接入 JWT 解码和会话表。 */
  public CurrentUser(JwtDecoder decoder, AuthTokenMapper tokens, SessionIssuer sessions) {
    if (decoder == null) {
      throw new IllegalArgumentException("decoder");
    }
    this.decoder = decoder;
    this.tokens = tokens;
    this.sessions = sessions;
  }

  /** token 无效、已撤销或超过空闲时间时拒绝。 */
  public Actor require(String authorization) {
    Jwt jwt;
    try {
      jwt = decoder.decode(AccessTokens.bearer(authorization));
    } catch (JwtException exception) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    if (jwt.getId() == null || jwt.getId().isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    AuthTokenEntity row = tokens.findByHash(SessionIssuer.sha256(jwt.getId()));
    sessions.touch(row);
    if (row.getUserId() == null || row.getId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    return new Actor(row.getUserId(), row.getId());
  }

  /** 当前会话。sessionId 用来标记设备列表里的本机，也用来远程下线。 */
  public record Actor(String userId, long sessionId) {
  }
}
