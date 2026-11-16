package com.digitalunionpay.trading.config;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 注册本进程的端口实现，以及对其它服务的 HTTP 客户端。 */
@Configuration
@MapperScan("com.digitalunionpay.trading.mapper")
public class TradingServiceConfiguration {

  /** 注册指向 ledger-service:8082 的清结算 HTTP 客户端。 */
  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }

  /** 注册指向 risk-service:8083 的风控 HTTP 客户端。 */
  @Bean
  RiskGate riskGate(@LoadBalanced RestClient.Builder builder) {
    return new HttpRiskGate(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.RISK)));
  }

  /** 注册 RocketMQ 通知端口，不调用 notify-service。 */
  @Bean
  NotificationPort notificationPort(StreamBridge bridge) {
    return new RocketNotificationPort(bridge);
  }
}
