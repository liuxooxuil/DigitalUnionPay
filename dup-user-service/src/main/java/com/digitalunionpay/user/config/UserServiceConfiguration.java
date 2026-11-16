package com.digitalunionpay.user.config;

import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.RocketNotificationPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 本模块 Mapper 扫描。通知发到 RocketMQ，不调用 notify-service。 */
@Configuration
@MapperScan("com.digitalunionpay.user.mapper")
public class UserServiceConfiguration {

  /** 注册 RocketMQ 通知端口，不指向 notify-service。 */
  @Bean
  NotificationPort notificationPort(StreamBridge bridge) {
    return new RocketNotificationPort(bridge);
  }
}
