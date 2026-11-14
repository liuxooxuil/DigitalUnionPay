package com.digitalunionpay.crypto;

import com.digitalunionpay.crypto.internal.SkeletonAddressRegistry;
import com.digitalunionpay.crypto.internal.SkeletonMpcSigner;
import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CryptoServiceConfiguration {

  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }

  @Bean
  AddressRegistry addressRegistry(TccLedger ledger) {
    return new SkeletonAddressRegistry(ledger);
  }

  @Bean
  MpcSigner mpcSigner() {
    return new SkeletonMpcSigner();
  }
}
