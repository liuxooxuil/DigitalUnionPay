package com.digitalunionpay.notify;

import com.digitalunionpay.common.Ids;

public record PublishNotification(String userId, NotifyEvent event, NotifyChannel channel) {

  public PublishNotification {
    userId = Ids.required(userId, "userId");
    if (event == null || channel == null) {
      throw new IllegalArgumentException("event and channel");
    }
  }
}
