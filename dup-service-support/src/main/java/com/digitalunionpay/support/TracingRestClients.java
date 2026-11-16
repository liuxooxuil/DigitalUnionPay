package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.seata.core.context.RootContext;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 带链路号、Seata XID 和 Sentinel 的服务间 HTTP 客户端。这里不加重试，避免付款和入账被调用两次。 */
public final class TracingRestClients {

  /** 禁止实例化。 */
  private TracingRestClients() {
  }

  /** baseUrl 形如 http://ledger-service，由 Nacos 负载均衡解析。 */
  public static RestClient create(RestClient.Builder builder, String baseUrl) {
    if (builder == null) {
      throw new IllegalArgumentException("builder");
    }
    Ids.required(baseUrl, "baseUrl");
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(8));
    return builder.clone()
        .baseUrl(baseUrl)
        .requestFactory(factory)
        .requestInterceptor(SentinelGuards::outbound)
        .requestInterceptor(TracingRestClients::copyTrace)
        .defaultStatusHandler(HttpStatusCode::isError, TracingRestClients::raise)
        .build();
  }

  /** 响应体为空时失败，避免把空结果当成成功。 */
  public static <T> T requireBody(T body, String name) {
    if (body == null) {
      throw new IllegalStateException(name);
    }
    return body;
  }

  /** 把当前链路号、访问 token 和 Seata XID 写到出站请求头。 */
  private static ClientHttpResponse copyTrace(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
      throws IOException {
    String traceId = MDC.get(TraceIdFilter.HEADER);
    if (traceId != null && !traceId.isBlank()) {
      request.getHeaders().set(TraceIdFilter.HEADER, traceId);
    }
    String token = AccessContext.get();
    if (token != null && !token.isBlank() && request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null) {
      request.getHeaders().setBearerAuth(token);
    }
    String xid = RootContext.getXID();
    if (xid != null && !xid.isBlank()) {
      request.getHeaders().set(RootContext.KEY_XID, xid);
    }
    return execution.execute(request, body);
  }

  /** 把错误响应转成 BusinessException。正文能认出的错误码优先使用。 */
  private static void raise(HttpRequest request, ClientHttpResponse response) throws IOException {
    InputStream stream = response.getBody();
    String raw = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    ErrorCode code = match(raw);
    if (code == null) {
      code = ErrorCode.UNAVAILABLE;
    }
    String message = raw.isBlank() ? response.getStatusCode().toString() : raw;
    throw new BusinessException(code, message);
  }

  /** 优先认 "code":"NAME"，其次认带引号的错误码名。 */
  private static ErrorCode match(String raw) {
    for (ErrorCode candidate : ErrorCode.values()) {
      if (raw.contains("\"code\":\"" + candidate.name() + "\"")) {
        return candidate;
      }
    }
    for (ErrorCode candidate : ErrorCode.values()) {
      if (raw.contains("\"" + candidate.name() + "\"")) {
        return candidate;
      }
    }
    return null;
  }
}
