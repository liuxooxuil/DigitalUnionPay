package com.digitalunionpay.notify.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天重试未发出的通知。工作尚未实现，不访问数据库。 */
@Component
public class NotificationDispatchJob {

  /** 每天重试未发出的通知。工作尚未实现，不访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void dispatch() {
  }
}
