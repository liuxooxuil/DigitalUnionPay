package com.digitalunionpay.support;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;

/** 从 Authorization 头里取出 Bearer token。 */
public final class AccessTokens {

  /** 禁止实例化。 */
  private AccessTokens() {
  }

  /** 头不是 Bearer，或后面没有 token 时拒绝。 */
  public static String bearer(String header) {
    if (header == null || header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    String token = header.substring(7).trim();
    if (token.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "token");
    }
    return token;
  }
}
