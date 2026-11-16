package com.digitalunionpay.funding.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 出入金对账。每天核对渠道到账和本地入金，尚未访问数据库。 */
@Component
public class FundingReconcileJob {

  /** 每天核对渠道到账和本地入金。尚未访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void reconcile() {
  }
}
