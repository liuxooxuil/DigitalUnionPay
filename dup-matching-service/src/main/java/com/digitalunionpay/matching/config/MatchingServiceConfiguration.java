package com.digitalunionpay.matching.config;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import com.digitalunionpay.valuation.HttpValuationPort;
import com.digitalunionpay.valuation.ValuationPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 本模块 Mapper 扫描，并注册估值和清结算的 HTTP 客户端。 */
@Configuration
@MapperScan("com.digitalunionpay.matching.mapper")
public class MatchingServiceConfiguration {

  /** 注册指向估值服务的 HTTP 客户端。 */
  @Bean
  ValuationPort valuationPort(@LoadBalanced RestClient.Builder builder) {
    return new HttpValuationPort(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.VALUATION)));
  }

  /** 注册指向清结算服务的 TCC HTTP 客户端。 */
  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }
}
