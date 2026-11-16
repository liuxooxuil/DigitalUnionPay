package com.digitalunionpay.common;

/** 标识字段的空值校验。业务流程不放在这里。 */
public final class Ids {

  /** 禁止实例化。 */
  private Ids() {
  }

  /** 拒绝 null 和空白，通过时原样返回。 */
  public static String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name);
    }
    return value;
  }
}
