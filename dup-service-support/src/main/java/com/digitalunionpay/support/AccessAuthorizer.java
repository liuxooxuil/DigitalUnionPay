package com.digitalunionpay.support;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/** 已登录的 JWT 还要命中 grants 声明里的方法和路径。 */
final class AccessAuthorizer implements AuthorizationManager<RequestAuthorizationContext> {

  /** 没有 JWT 或不匹配时拒绝。匿名请求由安全框架转成 401。 */
  @Override
  public AuthorizationResult authorize(
      Supplier<? extends @Nullable Authentication> authentication,
      RequestAuthorizationContext context) {
    Authentication current = authentication.get();
    if (!(current instanceof JwtAuthenticationToken jwtAuth)) {
      return new AuthorizationDecision(false);
    }
    Jwt jwt = jwtAuth.getToken();
    boolean allowed = AccessGrants.allows(jwt.getClaim("grants"), context.getRequest().getMethod(), RequestPaths.of(context.getRequest()));
    return new AuthorizationDecision(allowed);
  }
}
