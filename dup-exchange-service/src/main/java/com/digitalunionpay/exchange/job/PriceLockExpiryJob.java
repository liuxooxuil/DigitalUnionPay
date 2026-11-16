package com.digitalunionpay.exchange.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 锁价过期处理。每天处理过期锁价，尚未访问数据库。 */
@Component
public class PriceLockExpiryJob {

  /** 每天处理过期锁价。尚未访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void expire() {
  }
}
