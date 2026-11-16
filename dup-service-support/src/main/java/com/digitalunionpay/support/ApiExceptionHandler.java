package com.digitalunionpay.support;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.result.ApiResult;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** 把业务异常、校验失败和无法解析的请求转成统一失败外壳。 */
@RestControllerAdvice
public class ApiExceptionHandler {

  /** 按错误码选择 HTTP 状态，并把当前链路号放进响应。 */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResult<Void>> business(BusinessException exception) {
    return status(exception.code(), exception.getMessage());
  }

  /** 手写的参数检查失败返回 400。 */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResult<Void>> invalid(IllegalArgumentException exception) {
    return status(ErrorCode.BAD_REQUEST, exception.getMessage());
  }

  /** Bean 校验失败时只返回字段名，不回显提交值。 */
  @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
  public ResponseEntity<ApiResult<Void>> validation(Exception exception) {
    String message = "request";
    if (exception instanceof MethodArgumentNotValidException invalid) {
      message = invalid.getBindingResult().getFieldErrors().stream()
          .map(error -> error.getField())
          .distinct()
          .reduce((left, right) -> left + "," + right)
          .orElse("request");
    }
    return status(ErrorCode.VALIDATION, message);
  }

  /** 请求体不是合法 JSON 时返回 400，不把解析器原文回给调用方。 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResult<Void>> unreadable(HttpMessageNotReadableException exception) {
    return status(ErrorCode.BAD_REQUEST, "request");
  }

  /** 组装失败响应。消息和链路号为空时用空字符串。 */
  private static ResponseEntity<ApiResult<Void>> status(ErrorCode code, String message) {
    String traceId = MDC.get(TraceIdFilter.HEADER);
    ApiResult<Void> body = ApiResult.fail(code.name(), message, traceId);
    return ResponseEntity.status(httpStatus(code)).body(body);
  }

  /** 未实现 501，未登录 401，无权限 403，校验 400，冲突 409，依赖不可用 503，其余 422。 */
  private static int httpStatus(ErrorCode code) {
    return switch (code) {
      case PORT_NOT_IMPLEMENTED -> 501;
      case UNAUTHENTICATED -> 401;
      case FORBIDDEN -> 403;
      case BAD_REQUEST, VALIDATION -> 400;
      case CONFLICT -> 409;
      case UNAVAILABLE -> 503;
      default -> 422;
    };
  }
}
