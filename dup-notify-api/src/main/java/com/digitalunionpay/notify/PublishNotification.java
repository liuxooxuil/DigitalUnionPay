package com.digitalunionpay.notify;

import com.digitalunionpay.common.Ids;

/** 提交通知时的用户、事件和通道。 */
public record PublishNotification(String userId, NotifyEvent event, NotifyChannel channel) {

  /** 拒绝空白用户号，以及空的事件或通道。 */
  public PublishNotification {
    userId = Ids.required(userId, "userId");
    if (event == null || channel == null) {
      throw new IllegalArgumentException("event and channel");
    }
  }
}
