package com.digitalunionpay.notify;

/** 业务模块只提交事件，不关心具体通道实现。 */
public interface NotificationPort {

  /** 提交一条通知，不在这里实现具体通道。 */
  void publish(String userId, NotifyEvent event, NotifyChannel channel);
}
