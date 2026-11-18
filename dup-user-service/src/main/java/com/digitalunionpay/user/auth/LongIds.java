package com.digitalunionpay.user.auth;

import java.security.SecureRandom;

/** 给 JDBC 插入用的正数主键。 */
public final class LongIds {

  private static final SecureRandom RANDOM = new SecureRandom();

  /** 禁止实例化。 */
  private LongIds() {
  }

  /** 返回一个非 0 的正长整数。 */
  public static long next() {
    long value = RANDOM.nextLong() & Long.MAX_VALUE;
    return value == 0 ? 1 : value;
  }
}
