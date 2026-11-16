package com.digitalunionpay.matching;

/** 撮合服务的内部 HTTP 路径。 */
public final class MatchingPaths {
  /** 接受订单进入订单簿。 */
  public static final String ORDERS = "/internal/matching/orders";

  /** 禁止实例化。 */
  private MatchingPaths() {
  }
}
