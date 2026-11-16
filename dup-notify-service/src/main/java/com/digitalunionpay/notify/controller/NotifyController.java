package com.digitalunionpay.notify.controller;

import com.digitalunionpay.notify.NotifyPaths;
import com.digitalunionpay.notify.PublishNotification;
import com.digitalunionpay.notify.service.NotificationRecordService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class NotifyController {

  private final NotificationRecordService notifications;

  /** 接入本进程的服务入口。 */
  public NotifyController(NotificationRecordService notifications) {
    this.notifications = notifications;
  }

  /** 受理一条通知提交。 */
  @PostMapping(NotifyPaths.NOTIFICATIONS)
  public void publish(@RequestBody PublishNotification request) {
    notifications.publish(request.userId(), request.event(), request.channel());
  }
}
