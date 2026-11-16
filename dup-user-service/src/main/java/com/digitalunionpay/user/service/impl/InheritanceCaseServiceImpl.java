package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.user.mapper.InheritanceCaseMapper;
import com.digitalunionpay.user.service.InheritanceCaseService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class InheritanceCaseServiceImpl implements InheritanceCaseService {

  private final InheritanceCaseMapper inheritanceCaseMapper;

  private final NotificationPort notifications;

  /** 接入协作者和本聚合的 Mapper。 */
  public InheritanceCaseServiceImpl(
      InheritanceCaseMapper inheritanceCaseMapper, NotificationPort notifications) {
    if (notifications == null) {
      throw new IllegalArgumentException("notifications");
    }
    this.inheritanceCaseMapper = inheritanceCaseMapper;
    this.notifications = notifications;
  }

  /** 校验案件号、继承人和通知端口后拒绝立案。 */
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
