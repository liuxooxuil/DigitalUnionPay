package com.digitalunionpay.support;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.cfg.DateTimeFeature;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 各业务进程自动装上的发现、链路号、访问 token、负载均衡客户端、限流开关和 MyBatis-Plus。 */
@AutoConfiguration
@EnableDiscoveryClient
@Import({ApiExceptionHandler.class, MybatisPlusConfiguration.class})
public class ServiceSupportAutoConfiguration {

  /** 注册最高优先级的链路号过滤器。 */
  @Bean
  TraceIdFilter traceIdFilter() {
    return new TraceIdFilter();
  }

  /** 提供按服务名解析的 RestClient 构建器。 */
  @Bean
  @LoadBalanced
  RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
    return configurer.configure(RestClient.builder());
  }

  /** 用户服务自己提供检查器时不再远程调用。其他进程问 user-service。 */
  @Bean
  @ConditionalOnMissingBean(AccessChecker.class)
  AccessChecker accessChecker(RestClient.Builder loadBalancedRestClientBuilder) {
    RestClient client = TracingRestClients.create(loadBalancedRestClientBuilder, "http://user-service");
    return new HttpAccessChecker(client);
  }

  /** 注册内部接口的 token 拦截器。dup.auth.enabled=false 时拦截器放行。 */
  @Bean
  AccessTokenInterceptor accessTokenInterceptor(
      AccessChecker checker, @Value("${dup.auth.enabled:true}") boolean enabled) {
    return new AccessTokenInterceptor(checker, enabled);
  }

  /** 把 token 拦截器挂到全部 MVC 路径上。放行规则在拦截器内部。 */
  @Bean
  WebMvcConfigurer accessTokenWeb(AccessTokenInterceptor interceptor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**");
      }
    };
  }

  /** 业务时钟固定为 UTC。测试可以声明自己的 Clock 覆盖它。 */
  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock utcClock() {
    return Clock.systemUTC();
  }

  /** 时间写成 ISO-8601 字符串，不写成毫秒数字。 */
  @Bean
  JsonMapperBuilderCustomizer utcJson() {
    return builder -> builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  /** 每个进程一张幂等表。调用方要把 claim 和业务写放进同一个事务。 */
  @Bean
  IdempotencyStore idempotencyStore(JdbcTemplate jdbc, Clock utcClock) {
    return new JdbcIdempotencyStore(jdbc, utcClock);
  }

  /** 有 StreamBridge 时用它发领域事件，没有时发布会返回不可用。 */
  @Bean
  DomainEvents domainEvents(ObjectProvider<StreamBridge> bridges) {
    return new DomainEvents(bridges.getIfAvailable());
  }

  /** 按 seata.enabled 和 spring.cloud.sentinel.enabled 记下运行开关。 */
  @Bean
  AlibabaSwitches alibabaSwitches(
      @Value("${seata.enabled:true}") boolean seataEnabled,
      @Value("${spring.cloud.sentinel.enabled:true}") boolean sentinelEnabled) {
    return AlibabaSwitches.apply(seataEnabled, sentinelEnabled);
  }
}
