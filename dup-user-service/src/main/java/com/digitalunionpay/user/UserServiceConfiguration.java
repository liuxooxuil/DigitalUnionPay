package com.digitalunionpay.user;

import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.user.internal.SkeletonInheritanceCasePort;
import com.digitalunionpay.user.internal.SkeletonKycGate;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceConfiguration {

  @Bean
  KycGate kycGate() {
    return new SkeletonKycGate();
  }

  @Bean
  NotificationPort notificationPort(StreamBridge bridge) {
    return new RocketNotificationPort(bridge);
  }

  @Bean
  InheritanceCasePort inheritanceCasePort(NotificationPort notifications) {
    return new SkeletonInheritanceCasePort(notifications);
  }
}
