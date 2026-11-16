package com.digitalunionpay.notify;

import com.digitalunionpay.common.Ids;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;

/** 通知走 RocketMQ，不再同步调用 notify-service。 */
public final class RocketNotificationPort implements NotificationPort {

  public static final String BINDING = "domainEvents-out-0";

  private final StreamBridge bridge;

  /** 缺少 StreamBridge 时拒绝发送。 */
  public RocketNotificationPort(StreamBridge bridge) {
    if (bridge == null) {
      throw new IllegalArgumentException("bridge");
    }
    this.bridge = bridge;
  }

  /** 把通知事件发到 RocketMQ 绑定 domainEvents-out-0。 */
  @Override
  public void publish(String userId, NotifyEvent event, NotifyChannel channel) {
    Ids.required(userId, "userId");
    if (event == null || channel == null) {
      throw new IllegalArgumentException("event and channel");
    }
    boolean sent = bridge.send(BINDING, new DomainNotice(userId, event.name(), channel.name(), MDC.get("X-Trace-Id")));
    if (!sent) {
      throw new IllegalStateException("domain event was not sent");
    }
  }
}
