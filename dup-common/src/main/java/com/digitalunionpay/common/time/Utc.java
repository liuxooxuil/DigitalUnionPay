package com.digitalunionpay.common.time;

import java.time.Clock;
import java.time.Instant;

/** 业务时间一律用 UTC 的瞬间，不用本地时区的 LocalDateTime。 */
public final class Utc {

  /** 禁止实例化。 */
  private Utc() {
  }

  /** 系统 UTC 时钟。测试要固定时间时自己传入 Clock。 */
  public static Clock clock() {
    return Clock.systemUTC();
  }

  /** 按给定时钟取当前瞬间。clock 为空时用系统 UTC。 */
  public static Instant now(Clock clock) {
    return Instant.now(clock == null ? Clock.systemUTC() : clock);
  }
}
