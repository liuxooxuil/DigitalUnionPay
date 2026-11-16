package com.digitalunionpay.notify.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.notify.NotifyChannel;
import com.digitalunionpay.notify.NotifyEvent;
import com.digitalunionpay.notify.mapper.NotificationRecordMapper;
import com.digitalunionpay.notify.service.NotificationRecordService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class NotificationRecordServiceImpl implements NotificationRecordService {

  private final NotificationRecordMapper notificationRecordMapper;

  /** 接入本聚合的 Mapper。 */
  public NotificationRecordServiceImpl(NotificationRecordMapper notificationRecordMapper) {
    this.notificationRecordMapper = notificationRecordMapper;
  }

  /** 校验用户、事件和通道后拒绝发送。 */
  @Override
  public void publish(String userId, NotifyEvent event, NotifyChannel channel) {
    Ids.required(userId, "userId");
    if (event == null || channel == null) {
      throw new IllegalArgumentException("event and channel");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "NotificationPort.publish");
  }
}
