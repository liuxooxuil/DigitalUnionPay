package com.digitalunionpay.physical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 实物入金服务进程。设备读数在这里，记账通过 HTTP 调用清结算。 */
@SpringBootApplication
public class PhysicalServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(PhysicalServiceApplication.class, args);
  }
}
