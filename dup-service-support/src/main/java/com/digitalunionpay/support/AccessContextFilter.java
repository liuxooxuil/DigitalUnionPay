package com.digitalunionpay.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** 验签通过后把 JWT 原文留在线程上，给出站调用转发。 */
final class AccessContextFilter extends OncePerRequestFilter {

  /** 请求结束时清掉，避免线程复用串到下一次调用。 */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      AccessContext.set(jwt.getToken().getTokenValue());
    }
    try {
      chain.doFilter(request, response);
    } finally {
      AccessContext.clear();
    }
  }
}
