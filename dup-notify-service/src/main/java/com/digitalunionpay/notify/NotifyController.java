package com.digitalunionpay.notify;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotifyController {

  private final NotificationPort notifications;

  public NotifyController(NotificationPort notifications) {
    this.notifications = notifications;
  }

  @PostMapping(NotifyPaths.NOTIFICATIONS)
  public void publish(@RequestBody PublishNotification request) {
    notifications.publish(request.userId(), request.event(), request.channel());
  }
}
