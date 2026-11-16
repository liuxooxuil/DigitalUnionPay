package com.digitalunionpay.risk.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天复核挂起的风控记录。工作尚未实现，不访问数据库。 */
@Component
public class RiskReviewJob {

  /** 每天复核挂起的风控记录。工作尚未实现，不访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void review() {
  }
}
