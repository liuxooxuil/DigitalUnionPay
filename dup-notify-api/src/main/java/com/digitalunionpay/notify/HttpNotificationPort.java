package com.digitalunionpay.notify;

import org.springframework.web.client.RestClient;

/** 调用通知服务 8085 的 NotificationPort。 */
public final class HttpNotificationPort implements NotificationPort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpNotificationPort(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  /** 响应体为空时失败，避免把空结果当成成功。 */
  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  /** 调用 POST /internal/notifications 提交一条通知。 */
  @Override
  public void publish(String userId, NotifyEvent event, NotifyChannel channel) {
    client.post()
        .uri(NotifyPaths.NOTIFICATIONS)
        .body(new PublishNotification(userId, event, channel))
        .retrieve()
        .toBodilessEntity();
  }
}
