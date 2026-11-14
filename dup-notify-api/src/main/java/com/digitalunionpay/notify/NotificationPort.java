package com.digitalunionpay.notify;

/** 业务模块只提交事件，不关心具体通道实现。 */
public interface NotificationPort {

  void publish(String userId, NotifyEvent event, NotifyChannel channel);
}
