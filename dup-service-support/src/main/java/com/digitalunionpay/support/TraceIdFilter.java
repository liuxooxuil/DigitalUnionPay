package com.digitalunionpay.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/** 入站没有 X-Trace-Id 时生成一个，并写回响应。出站调用从同一条 MDC 往下传。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TraceIdFilter extends OncePerRequestFilter {

  /** 贯穿一次调用的链路号请求头。 */
  public static final String HEADER = "X-Trace-Id";

  /** 沿用入站链路号，没有则生成 UUID，并在请求结束后清掉 MDC。 */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String incoming = request.getHeader(HEADER);
    String traceId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming.trim();
    MDC.put(HEADER, traceId);
    response.setHeader(HEADER, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(HEADER);
    }
  }
}
