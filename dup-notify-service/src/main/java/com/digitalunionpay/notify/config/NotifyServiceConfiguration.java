package com.digitalunionpay.notify.config;

import com.digitalunionpay.notify.DomainNotice;
import com.digitalunionpay.notify.NotifyChannel;
import com.digitalunionpay.notify.NotifyEvent;
import com.digitalunionpay.notify.service.NotificationRecordService;
import java.util.function.Consumer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 本模块 Mapper 扫描，并消费领域事件。 */
@Configuration
@MapperScan("com.digitalunionpay.notify.mapper")
public class NotifyServiceConfiguration {

  /** 消费 digitalunionpay-domain-events，再交给通知端口。 */
  @Bean
  Consumer<DomainNotice> domainEventConsumer(NotificationRecordService notifications) {
    return notice -> notifications.publish(
        notice.userId(), NotifyEvent.valueOf(notice.event()), NotifyChannel.valueOf(notice.channel()));
  }
}
