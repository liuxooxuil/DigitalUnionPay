package com.digitalunionpay.support;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorBody> business(BusinessException exception) {
    int status = exception.code() == ErrorCode.PORT_NOT_IMPLEMENTED ? 501 : 422;
    return ResponseEntity.status(status).body(body(exception.code().name(), exception.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorBody> invalid(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(body("BAD_REQUEST", exception.getMessage()));
  }

  private static ErrorBody body(String code, String message) {
    return new ErrorBody(code, message == null ? "" : message);
  }
}
