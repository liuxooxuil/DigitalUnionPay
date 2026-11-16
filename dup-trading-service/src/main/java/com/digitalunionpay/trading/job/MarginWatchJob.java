package com.digitalunionpay.trading.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天盯保证金。尚未访问数据库。 */
@Component
public class MarginWatchJob {

  /** 每天盯保证金。尚未访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void watch() {
  }
}
