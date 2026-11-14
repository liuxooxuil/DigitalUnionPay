package com.digitalunionpay.notify;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.notify.internal.SkeletonNotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotifyServiceContextTest {

  @Autowired
  NotificationPort notifications;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonNotificationPort.class, notifications);
  }
}
