package com.digitalunionpay.physical;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.physical.internal.SkeletonPhysicalIntakePort;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PhysicalServiceConfiguration {

  @Bean
  ValuationPort valuationPort(@LoadBalanced RestClient.Builder builder) {
    return new HttpValuationPort(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.VALUATION)));
  }

  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }

  @Bean
  PhysicalIntakePort physicalIntakePort(ValuationPort valuation, TccLedger ledger) {
    return new SkeletonPhysicalIntakePort(valuation, ledger);
  }
}
