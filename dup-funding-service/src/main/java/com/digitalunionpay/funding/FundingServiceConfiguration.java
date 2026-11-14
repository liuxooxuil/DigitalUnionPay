package com.digitalunionpay.funding;

import com.digitalunionpay.funding.internal.SkeletonFiatCollectionPort;
import com.digitalunionpay.funding.internal.SkeletonFundingCoordinator;
import com.digitalunionpay.funding.internal.SkeletonWithdrawalPort;
import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.notify.NotificationPort;
import com.digitalunionpay.notify.RocketNotificationPort;
import com.digitalunionpay.risk.HttpRiskGate;
import com.digitalunionpay.risk.RiskGate;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import com.digitalunionpay.user.HttpKycGate;
import com.digitalunionpay.user.KycGate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FundingServiceConfiguration {

  @Bean
  FiatCollectionPort fiatCollectionPort() {
    return new SkeletonFiatCollectionPort();
  }

  @Bean
  WithdrawalPort withdrawalPort() {
    return new SkeletonWithdrawalPort();
  }

  @Bean
  KycGate kycGate(@LoadBalanced RestClient.Builder builder) {
    return new HttpKycGate(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.USER)));
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
  FundingCoordinator fundingCoordinator(
      KycGate kycGate, RiskGate riskGate, TccLedger ledger, NotificationPort notifications) {
    return new SkeletonFundingCoordinator(kycGate, riskGate, ledger, notifications);
  }
}
