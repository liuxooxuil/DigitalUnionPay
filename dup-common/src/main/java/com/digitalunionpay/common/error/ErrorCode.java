package com.digitalunionpay.common.error;

/** 跨服务共用的业务错误码。 */
public enum ErrorCode {
  /** 一笔分录借贷不平衡，或同一币种的 scale 不一致。 */
  UNBALANCED_JOURNAL,
  /** 端口仍是骨架实现，业务还没有接上。 */
  PORT_NOT_IMPLEMENTED,
  /** 没有带上有效访问 token。 */
  UNAUTHENTICATED,
  /** token 有效，但没有访问该接口的权限。 */
  FORBIDDEN,
  /** 请求体或查询参数无法解析。 */
  BAD_REQUEST,
  /** 字段校验没有通过。 */
  VALIDATION,
  /** 同一个幂等键已经执行过。 */
  CONFLICT,
  /** 依赖的服务或消息通道暂时不可用。 */
  UNAVAILABLE
}
