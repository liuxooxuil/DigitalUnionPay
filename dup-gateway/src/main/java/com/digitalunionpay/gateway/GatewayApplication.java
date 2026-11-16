package com.digitalunionpay.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 边缘网关进程。外部请求先过这里，资金和内部路径不向外部开放。 */
@SpringBootApplication
public class GatewayApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
