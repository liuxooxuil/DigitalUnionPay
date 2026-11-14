package com.digitalunionpay.trading;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import com.digitalunionpay.trading.internal.SkeletonTradingGateway;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TradingServiceConfiguration {

  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }

  @Bean
  RiskGate riskGate(@LoadBalanced RestClient.Builder builder) {
    return new HttpRiskGate(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.RISK)));
  }

  @Bean
  NotificationPort notificationPort(StreamBridge bridge) {
    return new RocketNotificationPort(bridge);
  }

  @Bean
  TradingGateway tradingGateway(TccLedger ledger, RiskGate riskGate, NotificationPort notifications) {
    return new SkeletonTradingGateway(ledger, riskGate, notifications);
  }
}
