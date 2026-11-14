package com.digitalunionpay.user.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.user.InheritanceCasePort;

public final class SkeletonInheritanceCasePort implements InheritanceCasePort {

  private final NotificationPort notifications;

  public SkeletonInheritanceCasePort(NotificationPort notifications) {
    if (notifications == null) {
      throw new IllegalArgumentException("notifications");
    }
    this.notifications = notifications;
  }

  @Override
  public void open(String caseId, String heirUserId) {
    Ids.required(caseId, "caseId");
    Ids.required(heirUserId, "heirUserId");
    if (notifications == null) {
      throw new IllegalStateException("notifications");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "InheritanceCasePort.open");
  }
}
