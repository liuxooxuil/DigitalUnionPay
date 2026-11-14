package com.digitalunionpay.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 撮合服务进程。订单簿在这里，成交资金通过 HTTP 调用清结算。 */
@SpringBootApplication
public class MatchingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MatchingServiceApplication.class, args);
  }
}
