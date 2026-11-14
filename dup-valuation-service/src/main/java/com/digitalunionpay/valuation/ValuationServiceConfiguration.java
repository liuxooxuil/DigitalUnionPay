package com.digitalunionpay.valuation;

import com.digitalunionpay.valuation.internal.SkeletonValuationPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValuationServiceConfiguration {

  @Bean
  ValuationPort valuationPort() {
    return new SkeletonValuationPort();
  }
}
