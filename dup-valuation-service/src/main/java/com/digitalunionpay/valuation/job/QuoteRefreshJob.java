package com.digitalunionpay.valuation.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天刷新汇率、金价和加密资产报价。工作尚未实现，不访问数据库。 */
@Component
public class QuoteRefreshJob {

  /** 每天刷新汇率、金价和加密资产报价。工作尚未实现，不访问数据库。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void refresh() {
  }
}
