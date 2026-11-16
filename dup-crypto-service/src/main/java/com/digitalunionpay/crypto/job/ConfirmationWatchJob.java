package com.digitalunionpay.crypto.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天盯链上确认数。尚未访问数据库。 */
@Component
public class ConfirmationWatchJob {

  /** 每天盯链上确认数。尚未访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void watch() {
  }
}
