package com.digitalunionpay.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 风控服务进程。限额和制裁筛查在业务入账之前经过这里。 */
@SpringBootApplication
public class RiskServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RiskServiceApplication.class, args);
  }
}
