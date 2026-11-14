package com.digitalunionpay.notify;

import org.springframework.web.client.RestClient;

public final class HttpNotificationPort implements NotificationPort {
  private final RestClient client;

  public HttpNotificationPort(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  @Override
  public void publish(String userId, NotifyEvent event, NotifyChannel channel) {
    client.post()
        .uri(NotifyPaths.NOTIFICATIONS)
        .body(new PublishNotification(userId, event, channel))
        .retrieve()
        .toBodilessEntity();
  }
}
