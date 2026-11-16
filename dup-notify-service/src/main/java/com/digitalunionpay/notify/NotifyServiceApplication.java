package com.digitalunionpay.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 通知服务进程。站内信、短信、邮件和 App Push 从这里发出。 */
@SpringBootApplication
public class NotifyServiceApplication {

  /** 启动本进程。 */
  public static void main(String[] args) {
    SpringApplication.run(NotifyServiceApplication.class, args);
  }
}
