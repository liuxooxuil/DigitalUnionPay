package com.digitalunionpay.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 兑换服务进程。锁价在这里，双边记账通过 HTTP 调用清结算。 */
@SpringBootApplication
public class ExchangeServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(ExchangeServiceApplication.class, args);
  }
}
