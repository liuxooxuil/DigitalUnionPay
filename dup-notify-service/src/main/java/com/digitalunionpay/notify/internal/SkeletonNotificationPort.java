package com.digitalunionpay.notify.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.NotifyChannel;
import com.digitalunionpay.notify.NotifyEvent;

public final class SkeletonNotificationPort implements NotificationPort {

  @Override
  public void publish(String userId, NotifyEvent event, NotifyChannel channel) {
    Ids.required(userId, "userId");
    if (event == null || channel == null) {
      throw new IllegalArgumentException("event and channel");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "NotificationPort.publish");
  }
}
