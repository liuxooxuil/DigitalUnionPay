package com.digitalunionpay.gateway;

import com.digitalunionpay.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 外部入口。先隔离路径和 IP，再验签名、JWT 和频率。Nonce 与计数只在本进程内存里。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EdgeGate extends OncePerRequestFilter {

  private static final String[] MONEY = {
    "funding", "ledger", "crypto", "exchange", "matching", "trading", "physical"
  };

  private final JwtDecoder decoder;

  private final java.time.Clock clock;

  private final String secret;

  private final Set<String> denied;

  private final int permits;

  private final long windowMillis;

  private final ConcurrentHashMap<String, Long> nonces = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  /** 密钥、拒绝名单和频率窗口来自配置。 */
  public EdgeGate(
      JwtDecoder decoder,
      java.time.Clock clock,
      @Value("${dup.edge.secret}") String secret,
      @Value("${dup.edge.deny-ips:}") String denyIps,
      @Value("${dup.edge.permits:30}") int permits,
      @Value("${dup.edge.window-millis:10000}") long windowMillis) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("secret");
    }
    this.decoder = decoder;
    this.clock = clock;
    this.secret = secret;
    this.permits = permits;
    this.windowMillis = windowMillis;
    this.denied = ConcurrentHashMap.newKeySet();
    if (denyIps != null) {
      for (String ip : denyIps.split(",")) {
        if (!ip.isBlank()) {
          denied.add(ip.trim());
        }
      }
    }
  }

  /** 内部路径和资金前缀直接拒绝。登录只限 IP 和频率。探测接口要签名和权限。 */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String trace = request.getHeader("X-Trace-Id");
    if (trace == null || trace.isBlank()) {
      trace = UUID.randomUUID().toString();
    }
    response.setHeader("X-Trace-Id", trace);
    String path = path(request);
    String ip = clientIp(request);
    if (denied.contains(ip)) {
      EdgeResponses.write(response, ErrorCode.FORBIDDEN, "external", trace);
      return;
    }
    if (path.startsWith("/internal")) {
      EdgeResponses.write(response, ErrorCode.FORBIDDEN, "internal", trace);
      return;
    }
    if (money(path)) {
      EdgeResponses.write(response, ErrorCode.FORBIDDEN, "external", trace);
      return;
    }
    if (actuator(path) || "/error".equals(path)) {
      chain.doFilter(request, response);
      return;
    }
    if (login(request, path)) {
      if (!allow(ip)) {
        EdgeResponses.write(response, ErrorCode.RATE_LIMITED, "user", trace);
        return;
      }
      chain.doFilter(request, response);
      return;
    }
    if (!ping(request, path)) {
      EdgeResponses.write(response, ErrorCode.FORBIDDEN, "external", trace);
      return;
    }
    byte[] body = request.getInputStream().readAllBytes();
    if (!signature(request, path, body, response, trace)) {
      return;
    }
    String subject = subject(request, path, response, trace);
    if (subject == null) {
      return;
    }
    if (!allow(subject)) {
      EdgeResponses.write(response, ErrorCode.RATE_LIMITED, "user", trace);
      return;
    }
    chain.doFilter(new CachedBody(request, body), response);
  }

  /** 缺头、时间窗外、摘要不对或 nonce 重放都拒绝。 */
  private boolean signature(
      HttpServletRequest request, String path, byte[] body, HttpServletResponse response, String trace)
      throws IOException {
    String timestamp = request.getHeader(EdgeSigner.TIME);
    String nonce = request.getHeader(EdgeSigner.NONCE);
    String sign = request.getHeader(EdgeSigner.SIGN);
    if (timestamp == null || nonce == null || nonce.isBlank() || sign == null || sign.isBlank()) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "signature", trace);
      return false;
    }
    long sent;
    try {
      sent = Long.parseLong(timestamp);
    } catch (NumberFormatException exception) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "signature", trace);
      return false;
    }
    if (Math.abs(clock.millis() - sent) > EdgeSigner.SKEW_MILLIS) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "timestamp", trace);
      return false;
    }
    String expected = EdgeSigner.sign(secret, request.getMethod(), path, timestamp, nonce, body);
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sign.getBytes(StandardCharsets.UTF_8))) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "signature", trace);
      return false;
    }
    long now = clock.millis();
    if (nonces.size() > 10_000) {
      nonces.entrySet().removeIf(entry -> now - entry.getValue() > EdgeSigner.SKEW_MILLIS);
    }
    if (nonces.putIfAbsent(nonce, now) != null) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "nonce", trace);
      return false;
    }
    return true;
  }

  /** 令牌无效返回 null。权限不够也返回 null，并已写出响应。 */
  private String subject(HttpServletRequest request, String path, HttpServletResponse response, String trace)
      throws IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ") || header.length() < 8) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "token", trace);
      return null;
    }
    Jwt jwt;
    try {
      jwt = decoder.decode(header.substring("Bearer ".length()).trim());
    } catch (JwtException exception) {
      EdgeResponses.write(response, ErrorCode.UNAUTHENTICATED, "token", trace);
      return null;
    }
    if (!EdgeGrants.allows(jwt.getClaim("grants"), request.getMethod(), path)) {
      EdgeResponses.write(response, ErrorCode.FORBIDDEN, "path", trace);
      return null;
    }
    String subject = jwt.getSubject();
    return subject == null || subject.isBlank() ? ipFallback(request) : subject;
  }

  /** 当前窗口内超过次数就拒绝。同一键的更新是原子的。 */
  private boolean allow(String key) {
    long now = clock.millis();
    Window window = windows.compute(key, (name, existing) -> {
      if (existing == null || now - existing.start >= windowMillis) {
        return new Window(now, 1);
      }
      existing.count++;
      return existing;
    });
    return window.count <= permits;
  }

  /** 取 X-Forwarded-For 的第一跳，否则用连接地址。本进程就是边缘时才信任这个头。 */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      String first = comma < 0 ? forwarded : forwarded.substring(0, comma);
      if (!first.isBlank()) {
        return first.trim();
      }
    }
    String remote = request.getRemoteAddr();
    return remote == null ? "" : remote;
  }

  private static String path(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri == null || uri.isBlank() ? "/" : uri;
  }

  private static boolean login(HttpServletRequest request, String path) {
    return "POST".equals(request.getMethod()) && "/api/auth/login".equals(path);
  }

  private static boolean ping(HttpServletRequest request, String path) {
    return "GET".equals(request.getMethod()) && "/api/edge/ping".equals(path);
  }

  private static boolean actuator(String path) {
    return "/actuator".equals(path) || path.startsWith("/actuator/");
  }

  private static boolean money(String path) {
    for (String name : MONEY) {
      String prefix = "/api/" + name;
      if (path.equals(prefix) || path.startsWith(prefix + "/")) {
        return true;
      }
    }
    return false;
  }

  private static String ipFallback(HttpServletRequest request) {
    return clientIp(request);
  }

  /** 一个频率窗口。 */
  private static final class Window {

    private final long start;

    private int count;

    private Window(long start, int count) {
      this.start = start;
      this.count = count;
    }
  }
}
