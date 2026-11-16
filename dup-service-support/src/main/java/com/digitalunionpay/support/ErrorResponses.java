package com.digitalunionpay.support;

import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.result.ApiResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

/** 失败响应的 HTTP 状态和 JSON。过滤器和控制器共用，避免两套状态码。 */
final class ErrorResponses {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** 禁止实例化。 */
  private ErrorResponses() {
  }

  /** 给控制器异常处理用的失败响应。 */
  static ResponseEntity<ApiResult<Void>> of(ErrorCode code, String message) {
    return ResponseEntity.status(status(code)).body(body(code, message));
  }

  /** 过滤器里直接写出失败 JSON。Spring Security 的拒绝不经过控制器异常处理。 */
  static void write(HttpServletResponse response, ErrorCode code, String message) throws IOException {
    response.setStatus(status(code));
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    JSON.writeValue(response.getOutputStream(), body(code, message));
  }

  /** 未实现 501，未登录 401，无权限 403，校验 400，冲突 409，限流 429，依赖不可用 503，其余 422。 */
  private static int status(ErrorCode code) {
    return switch (code) {
      case PORT_NOT_IMPLEMENTED -> 501;
      case UNAUTHENTICATED -> 401;
      case FORBIDDEN -> 403;
      case BAD_REQUEST, VALIDATION -> 400;
      case CONFLICT -> 409;
      case RATE_LIMITED -> 429;
      case UNAVAILABLE -> 503;
      default -> 422;
    };
  }

  /** 链路号取当前 MDC。没有时用空字符串。 */
  private static ApiResult<Void> body(ErrorCode code, String message) {
    return ApiResult.fail(code.name(), message, MDC.get(TraceIdFilter.HEADER));
  }
}
