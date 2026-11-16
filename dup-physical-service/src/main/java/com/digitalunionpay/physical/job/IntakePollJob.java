package com.digitalunionpay.physical.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天拉取设备读数。尚未访问数据库。 */
@Component
public class IntakePollJob {

  /** 每天拉取设备读数。尚未访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void poll() {
  }
}
