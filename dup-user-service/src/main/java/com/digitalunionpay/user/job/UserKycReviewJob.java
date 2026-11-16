package com.digitalunionpay.user.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天补查停在审核中的 KYC。工作尚未实现，不访问数据库。 */
@Component
public class UserKycReviewJob {

  /** 每天补查停在审核中的 KYC。工作尚未实现，不访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void review() {
  }
}
