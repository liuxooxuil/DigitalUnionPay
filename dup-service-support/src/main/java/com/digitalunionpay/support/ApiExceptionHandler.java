package com.digitalunionpay.support;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 把业务异常和非法入参转成统一的错误体。 */
@RestControllerAdvice
public class ApiExceptionHandler {

  /** 未实现返回 501，未登录返回 401，无权限返回 403，其余业务错误返回 422。 */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorBody> business(BusinessException exception) {
    int status = 422;
    if (exception.code() == ErrorCode.PORT_NOT_IMPLEMENTED) {
      status = 501;
    } else if (exception.code() == ErrorCode.UNAUTHENTICATED) {
      status = 401;
    } else if (exception.code() == ErrorCode.FORBIDDEN) {
      status = 403;
    }
    return ResponseEntity.status(status).body(body(exception.code().name(), exception.getMessage()));
  }

  /** 入参不合法时返回 400。 */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorBody> invalid(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(body("BAD_REQUEST", exception.getMessage()));
  }

  /** 组装错误体。消息为空时用空字符串，避免响应里出现 null。 */
  private static ErrorBody body(String code, String message) {
    return new ErrorBody(code, message == null ? "" : message);
  }
}
