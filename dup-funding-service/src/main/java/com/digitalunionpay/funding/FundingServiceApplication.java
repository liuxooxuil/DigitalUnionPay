package com.digitalunionpay.funding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 出入金服务进程。法币渠道在这里，入账通过 HTTP 调用清结算。 */
@SpringBootApplication
public class FundingServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(FundingServiceApplication.class, args);
  }
}
