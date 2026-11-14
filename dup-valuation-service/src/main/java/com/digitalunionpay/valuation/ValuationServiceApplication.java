package com.digitalunionpay.valuation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 估值服务进程。汇率、金价和加密资产价格只从这里读出。 */
@SpringBootApplication
public class ValuationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ValuationServiceApplication.class, args);
  }
}
