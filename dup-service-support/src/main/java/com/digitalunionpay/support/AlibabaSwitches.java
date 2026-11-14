package com.digitalunionpay.support;

/** 测试配置关掉 Seata 和 Sentinel 时，调用路径不再去连对应进程。 */
public final class AlibabaSwitches {

  private static volatile boolean seata;
  private static volatile boolean sentinel;

  private AlibabaSwitches() {
  }

  public static AlibabaSwitches apply(boolean seataEnabled, boolean sentinelEnabled) {
    seata = seataEnabled;
    sentinel = sentinelEnabled;
    return new AlibabaSwitches();
  }

  public static boolean seata() {
    return seata;
  }

  public static boolean sentinel() {
    return sentinel;
  }
}
