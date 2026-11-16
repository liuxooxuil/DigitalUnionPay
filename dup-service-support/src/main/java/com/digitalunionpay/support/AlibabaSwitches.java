package com.digitalunionpay.support;

/** 测试配置关掉 Seata 和 Sentinel 时，调用路径不再去连对应进程。 */
public final class AlibabaSwitches {

  private static volatile boolean seata;
  private static volatile boolean sentinel;

  /** 禁止业务代码自行构造。开关只通过 apply 写入。 */
  private AlibabaSwitches() {
  }

  /** 按配置记下 Seata 和 Sentinel 是否启用，并交给 Spring 管理生命周期。 */
  public static AlibabaSwitches apply(boolean seataEnabled, boolean sentinelEnabled) {
    seata = seataEnabled;
    sentinel = sentinelEnabled;
    return new AlibabaSwitches();
  }

  /** Seata 是否启用。关闭时全局事务直接执行业务体。 */
  public static boolean seata() {
    return seata;
  }

  /** Sentinel 是否启用。关闭时出站调用不再申请资源。 */
  public static boolean sentinel() {
    return sentinel;
  }
}
