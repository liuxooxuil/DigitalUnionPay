package com.digitalunionpay.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableDiscoveryClient
@Import(ApiExceptionHandler.class)
public class ServiceSupportAutoConfiguration {

  @Bean
  TraceIdFilter traceIdFilter() {
    return new TraceIdFilter();
  }

  @Bean
  @LoadBalanced
  RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
    return configurer.configure(RestClient.builder());
  }

  @Bean
  AlibabaSwitches alibabaSwitches(
      @Value("${seata.enabled:true}") boolean seataEnabled,
      @Value("${spring.cloud.sentinel.enabled:true}") boolean sentinelEnabled) {
    return AlibabaSwitches.apply(seataEnabled, sentinelEnabled);
  }
}
