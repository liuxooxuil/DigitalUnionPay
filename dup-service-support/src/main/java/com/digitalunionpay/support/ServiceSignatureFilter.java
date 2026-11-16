package com.digitalunionpay.support;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.common.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/** 除登录和健康检查外，内部调用要带服务签名。关闭时直接放行。 */
final class ServiceSignatureFilter extends OncePerRequestFilter {

  private final ServiceCall calls;

  private final boolean enabled;

  /** enabled 为 false 时测试请求不必签名。 */
  ServiceSignatureFilter(ServiceCall calls, boolean enabled) {
    if (calls == null) {
      throw new IllegalArgumentException("calls");
    }
    this.calls = calls;
    this.enabled = enabled;
  }

  /** 先读完报文体再验签，并把同一份报文体交给后面的控制器。 */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!enabled || open(request)) {
      chain.doFilter(request, response);
      return;
    }
    byte[] body = request.getInputStream().readAllBytes();
    try {
      calls.verify(
          request.getMethod(),
          RequestPaths.of(request),
          request.getHeader(ServiceCall.TIME),
          request.getHeader(ServiceCall.NONCE),
          body,
          request.getHeader(ServiceCall.SIGN));
    } catch (BusinessException exception) {
      ErrorResponses.write(response, exception.code(), exception.getMessage());
      return;
    }
    chain.doFilter(new CachedBodyRequest(request, body), response);
  }

  /** 登录由人发起，不要求服务签名。 */
  private static boolean open(HttpServletRequest request) {
    String path = RequestPaths.of(request);
    if (path.startsWith("/actuator/") || "/actuator".equals(path) || "/error".equals(path)) {
      return true;
    }
    return "POST".equals(request.getMethod()) && AuthPaths.LOGIN.equals(path);
  }

  /** 过滤器读过的报文体再提供给控制器。 */
  private static final class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    private CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body == null ? new byte[0] : body;
    }

    /** 每次调用都从缓存重新读。 */
    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream input = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
        }

        @Override
        public int read() {
          return input.read();
        }
      };
    }

    /** 字符方式读取同样走缓存。 */
    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}
