package com.digitalunionpay.common.money;

/**
 * 金额用最小单位的整数，避免浮点。同一币种在一笔记账里必须使用相同的 scale。
 */
public record Money(long minorUnits, int scale, String currency) {

  /** 拒绝负数、越界的 scale 和不合规的币种代码。 */
  public Money {
    if (minorUnits < 0) {
      throw new IllegalArgumentException("minorUnits must be >= 0");
    }
    if (scale < 0 || scale > 18) {
      throw new IllegalArgumentException("scale must be between 0 and 18");
    }
    if (currency == null || !currency.matches("[A-Z0-9]{3,10}")) {
      throw new IllegalArgumentException("currency must be 3-10 uppercase letters or digits");
    }
  }
}
