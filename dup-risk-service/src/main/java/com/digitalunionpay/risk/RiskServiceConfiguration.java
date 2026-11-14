package com.digitalunionpay.risk;

import com.digitalunionpay.risk.internal.SkeletonRiskGate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiskServiceConfiguration {

  @Bean
  RiskGate riskGate() {
    return new SkeletonRiskGate();
  }
}
