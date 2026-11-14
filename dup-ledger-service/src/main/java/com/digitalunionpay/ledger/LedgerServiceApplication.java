package com.digitalunionpay.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 清结算服务进程。复式分录和 TCC 只从这里进入。 */
@SpringBootApplication
public class LedgerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LedgerServiceApplication.class, args);
  }
}
