package com.digitalunionpay.notify;

/** RocketMQ 上的通知事件。topic 为 digitalunionpay-domain-events。 */
public record DomainNotice(String userId, String event, String channel, String traceId) {
}
