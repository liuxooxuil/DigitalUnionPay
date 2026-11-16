package com.digitalunionpay.support;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.result.ApiResult;
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
    return ErrorResponses.of(exception.code(), exception.getMessage());
  }

  /** 手写的参数检查失败返回 400。 */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResult<Void>> invalid(IllegalArgumentException exception) {
    return ErrorResponses.of(ErrorCode.BAD_REQUEST, exception.getMessage());
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
    return ErrorResponses.of(ErrorCode.VALIDATION, message);
  }

  /** 请求体不是合法 JSON 时返回 400，不把解析器原文回给调用方。 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResult<Void>> unreadable(HttpMessageNotReadableException exception) {
    return ErrorResponses.of(ErrorCode.BAD_REQUEST, "request");
  }
}
