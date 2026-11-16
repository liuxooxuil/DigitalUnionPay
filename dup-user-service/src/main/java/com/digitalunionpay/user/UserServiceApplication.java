package com.digitalunionpay.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 用户服务进程。注册、会话、RBAC、KYC 和继承在这里，资金变动不在这个进程里。 */
@SpringBootApplication
public class UserServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
}
