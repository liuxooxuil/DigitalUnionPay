package com.digitalunionpay.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 自营交易服务进程。MT5 网关在这里，保证金和盈亏通过 HTTP 调用清结算。 */
@SpringBootApplication
public class TradingServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(TradingServiceApplication.class, args);
  }
}
