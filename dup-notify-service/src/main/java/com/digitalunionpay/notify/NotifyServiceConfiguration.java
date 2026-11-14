package com.digitalunionpay.notify;

import com.digitalunionpay.notify.internal.SkeletonNotificationPort;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotifyServiceConfiguration {

  @Bean
  NotificationPort notificationPort() {
    return new SkeletonNotificationPort();
  }

  @Bean
  Consumer<DomainNotice> domainEventConsumer(NotificationPort notifications) {
    return notice -> notifications.publish(
        notice.userId(), NotifyEvent.valueOf(notice.event()), NotifyChannel.valueOf(notice.channel()));
  }
}
