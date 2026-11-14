package com.digitalunionpay.exchange;

import com.digitalunionpay.exchange.internal.SkeletonExchangePort;
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
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeServiceConfiguration {

  @Bean
  ValuationPort valuationPort(@LoadBalanced RestClient.Builder builder) {
    return new HttpValuationPort(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.VALUATION)));
  }

  @Bean
  RiskGate riskGate(@LoadBalanced RestClient.Builder builder) {
    return new HttpRiskGate(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.RISK)));
  }

  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }

  @Bean
  NotificationPort notificationPort(StreamBridge bridge) {
    return new RocketNotificationPort(bridge);
  }

  @Bean
  ExchangePort exchangePort(ValuationPort valuation, RiskGate riskGate, TccLedger ledger, NotificationPort notifications) {
    return new SkeletonExchangePort(valuation, riskGate, ledger, notifications);
  }
}
