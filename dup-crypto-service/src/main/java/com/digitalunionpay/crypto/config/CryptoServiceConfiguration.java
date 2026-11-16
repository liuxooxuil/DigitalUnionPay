package com.digitalunionpay.crypto.config;

import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 注册本进程的端口实现，以及对其它服务的 HTTP 客户端。 */
@Configuration
@MapperScan("com.digitalunionpay.crypto.mapper")
public class CryptoServiceConfiguration {

  /** 注册指向 ledger-service:8082 的清结算 HTTP 客户端。 */
  @Bean
  TccLedger tccLedger(@LoadBalanced RestClient.Builder builder) {
    return new HttpTccLedger(TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER)));
  }
}
