package com.digitalunionpay.common;

/** 服务之间约定的请求头。链路号在 TraceIdFilter，不放在这里。 */
public final class RequestHeaders {

  /** 同一次业务动作重复提交时携带的键。 */
  public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  /** 禁止实例化。 */
  private RequestHeaders() {
  }
}
