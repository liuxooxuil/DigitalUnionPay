package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.RequestHeaders;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/** 领域事件只走这一条 RocketMQ 绑定。发送失败不重试，避免一笔交易发出两次。 */
public final class DomainEvents {

  /** 当前事件结构版本。消费者遇到更高版本时转入死信，不按旧结构硬解。 */
  public static final int EVENT_VERSION = 1;

  /** 与 digitalunionpay-nacos.yml 里的绑定名一致。 */
  static final String BINDING = "domainEvents-out-0";

  private final StreamBridge bridge;

  /** bridge 为空时发布会失败，测试和未启用消息的进程可以这样装。 */
  public DomainEvents(StreamBridge bridge) {
    this.bridge = bridge;
  }

  /** 带上事件类型、链路号和幂等键。通道没接上或发送返回 false 时抛 UNAVAILABLE。 */
  public void publish(String type, Object payload, String idempotencyKey) {
    Ids.required(type, "type");
    if (payload == null) {
      throw new IllegalArgumentException("payload");
    }
    if (bridge == null) {
      throw new BusinessException(ErrorCode.UNAVAILABLE, type);
    }
    MessageBuilder<Object> builder = MessageBuilder.withPayload(payload)
        .setHeader("eventType", type)
        .setHeader("eventId", UUID.randomUUID().toString())
        .setHeader("eventVersion", EVENT_VERSION)
        .setHeader("orderKey", idempotencyKey == null ? "" : idempotencyKey);
    String traceId = MDC.get(TraceIdFilter.HEADER);
    if (traceId != null && !traceId.isBlank()) {
      builder.setHeader(TraceIdFilter.HEADER, traceId);
    }
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      builder.setHeader(RequestHeaders.IDEMPOTENCY_KEY, idempotencyKey);
    }
    Message<Object> message = builder.build();
    if (!bridge.send(BINDING, message)) {
      throw new BusinessException(ErrorCode.UNAVAILABLE, type);
    }
  }
}
