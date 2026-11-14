package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.seata.core.context.RootContext;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

public final class TracingRestClients {

  private TracingRestClients() {
  }

  /** baseUrl 形如 http://ledger-service，由 Nacos 负载均衡解析。 */
  public static RestClient create(RestClient.Builder builder, String baseUrl) {
    if (builder == null) {
      throw new IllegalArgumentException("builder");
    }
    Ids.required(baseUrl, "baseUrl");
    return builder.clone()
        .baseUrl(baseUrl)
        .requestInterceptor(SentinelGuards::outbound)
        .requestInterceptor(TracingRestClients::copyTrace)
        .defaultStatusHandler(HttpStatusCode::isError, TracingRestClients::raise)
        .build();
  }

  public static <T> T requireBody(T body, String name) {
    if (body == null) {
      throw new IllegalStateException(name);
    }
    return body;
  }

  private static ClientHttpResponse copyTrace(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
      throws IOException {
    String traceId = MDC.get(TraceIdFilter.HEADER);
    if (traceId != null && !traceId.isBlank()) {
      request.getHeaders().set(TraceIdFilter.HEADER, traceId);
    }
    String xid = RootContext.getXID();
    if (xid != null && !xid.isBlank()) {
      request.getHeaders().set(RootContext.KEY_XID, xid);
    }
    return execution.execute(request, body);
  }

  private static void raise(HttpRequest request, ClientHttpResponse response) throws IOException {
    InputStream stream = response.getBody();
    String raw = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    ErrorCode code = ErrorCode.PORT_NOT_IMPLEMENTED;
    for (ErrorCode candidate : ErrorCode.values()) {
      if (raw.contains(candidate.name())) {
        code = candidate;
        break;
      }
    }
    String message = raw.isBlank() ? response.getStatusCode().toString() : raw;
    throw new BusinessException(code, message);
  }
}
