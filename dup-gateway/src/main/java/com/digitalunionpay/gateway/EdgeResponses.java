package com.digitalunionpay.gateway;

import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.result.ApiResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/** 边缘拒绝的 HTTP 状态和 JSON。成功响应不走这里。 */
final class EdgeResponses {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private EdgeResponses() {
  }

  /** 写出失败 JSON。traceId 由调用方传入。 */
  static void write(HttpServletResponse response, ErrorCode code, String message, String traceId) throws IOException {
    response.setStatus(status(code));
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("X-Trace-Id", traceId == null ? "" : traceId);
    JSON.writeValue(response.getOutputStream(), ApiResult.fail(code.name(), message, traceId));
  }

  /** 未登录 401，无权限 403，频率 429，依赖不可用 503。 */
  static int status(ErrorCode code) {
    return switch (code) {
      case UNAUTHENTICATED -> 401;
      case FORBIDDEN -> 403;
      case RATE_LIMITED -> 429;
      case UNAVAILABLE -> 503;
      default -> 422;
    };
  }
}
