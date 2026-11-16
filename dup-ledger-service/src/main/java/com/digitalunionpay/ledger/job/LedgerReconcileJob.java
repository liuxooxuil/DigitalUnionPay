package com.digitalunionpay.ledger.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天核对冻结和分录。工作尚未实现，不访问数据库。 */
@Component
public class LedgerReconcileJob {

  /** 每天核对冻结和分录。工作尚未实现，不访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void reconcile() {
  }
}
