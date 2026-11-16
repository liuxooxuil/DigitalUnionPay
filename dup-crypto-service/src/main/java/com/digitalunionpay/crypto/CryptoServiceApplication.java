package com.digitalunionpay.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 加密资产服务进程。地址和托管签名在这里，客户余额通过 HTTP 调用清结算。 */
@SpringBootApplication
public class CryptoServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(CryptoServiceApplication.class, args);
  }
}
