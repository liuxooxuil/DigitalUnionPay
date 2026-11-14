package com.digitalunionpay.ledger;

import com.digitalunionpay.ledger.internal.SkeletonTccLedger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerServiceConfiguration {

  @Bean
  TccLedger tccLedger() {
    return new SkeletonTccLedger();
  }
}
