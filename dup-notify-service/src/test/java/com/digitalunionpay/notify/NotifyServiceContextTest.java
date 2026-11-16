package com.digitalunionpay.notify;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.notify.service.impl.NotificationRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查通知服务上下文是否装上本地通知端口。 */
@SpringBootTest
@ActiveProfiles("test")
class NotifyServiceContextTest {

  @Autowired
  NotificationPort notifications;

  /** 确认通知端口是占位实现。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(NotificationRecordServiceImpl.class, notifications);
  }
}
