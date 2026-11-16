package com.digitalunionpay.exchange.config;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 本模块 Mapper 扫描，并注册估值、风控、清结算的 HTTP 客户端和 RocketMQ 通知端口。 */
@Configuration
@MapperScan("com.digitalunionpay.exchange.mapper")
public class ExchangeServiceConfiguration {

  /** 注册指向估值服务的 HTTP 客户端。 */
  @Bean
  ValuationPort valuationPort(@LoadBalanced RestClient.Builder builder) {
    return new HttpValuationPort(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.VALUATION)));
  }

  /** 注册指向风控服务的 HTTP 客户端。 */
  @Bean
  RiskGate riskGate(@LoadBalanced RestClient.Builder builder) {
    return new HttpRiskGate(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.RISK)));
  }

  /** 注册指向清结算服务的 TCC HTTP 客户端。 */
  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }

  /** 注册走 RocketMQ 的通知端口。 */
  @Bean
  NotificationPort notificationPort(StreamBridge bridge) {
    return new RocketNotificationPort(bridge);
  }
}
