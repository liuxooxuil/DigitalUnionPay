package com.digitalunionpay.matching.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 订单撮合。每天驱动未成交订单，尚未访问数据库。 */
@Component
public class OrderMatchJob {

  /** 每天驱动未成交订单。尚未访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void match() {
  }
}
