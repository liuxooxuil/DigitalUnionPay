package com.digitalunionpay.support;

/** 资源上写了归属人时，调用方必须是同一个人。没有归属人或没有调用方时不拦截服务间调用。 */
public final class Abac {

  /** 禁止实例化。 */
  private Abac() {
  }

  /** 两边都有值且不相等时拒绝。 */
  public static boolean permits(String actor, String owner) {
    if (actor == null || actor.isBlank() || owner == null || owner.isBlank()) {
      return true;
    }
    return actor.equals(owner);
  }
}
