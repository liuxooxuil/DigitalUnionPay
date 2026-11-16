package com.digitalunionpay.common.error;

/** 带错误码的业务异常。由各服务的统一异常处理转成 HTTP 响应。 */
public final class BusinessException extends RuntimeException {

  private final ErrorCode code;

  /** 记录错误码和说明。说明会作为异常消息返回给调用方。 */
  public BusinessException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  /** 返回业务错误码，供异常处理选择 HTTP 状态。 */
  public ErrorCode code() {
    return code;
  }
}
