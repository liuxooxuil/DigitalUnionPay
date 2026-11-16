package com.digitalunionpay.common.result;

/** 失败响应的统一外壳。成功时各接口仍直接返回自己的资源，避免再包一层。 */
public record ApiResult<T>(String code, String message, T data, String traceId) {

  /** 业务成功时使用的 code。 */
  public static final String OK = "OK";

  /** 补上空白消息和链路号，避免响应里出现 null。 */
  public ApiResult {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code");
    }
    message = message == null ? "" : message;
    traceId = traceId == null ? "" : traceId;
  }

  /** 成功结果。data 可以是 null。 */
  public static <T> ApiResult<T> ok(T data, String traceId) {
    return new ApiResult<>(OK, "", data, traceId);
  }

  /** 失败结果。data 固定为空。 */
  public static <T> ApiResult<T> fail(String code, String message, String traceId) {
    return new ApiResult<>(code, message, null, traceId);
  }
}
