package com.digitalunionpay.support;

import com.digitalunionpay.common.RequestHeaders;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import tools.jackson.databind.cfg.DateTimeFeature;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

/** 各业务进程自动装上的发现、链路号、访问 token、负载均衡客户端、限流开关和 MyBatis-Plus。 */
@AutoConfiguration
@AutoConfigureBefore({UserDetailsServiceAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@EnableDiscoveryClient
@EnableScheduling
@Import({ApiExceptionHandler.class, MybatisPlusConfiguration.class, AccessSecurity.class})
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
  DomainEvents publishedDomainEvents(ObjectProvider<StreamBridge> bridges) {
    return new DomainEvents(bridges.getIfAvailable());
  }

  /** 登录签发和各进程验签用同一把 HS256 密钥。 */
  @Bean
  JwtEncoder jwtEncoder(@Value("${dup.auth.jwt-secret:DigitalUnionPayJwtSecretKey32B!!}") String secret) {
    return AccessJwt.encoder(secret);
  }

  /** 资源服务器只验签，不在这里查用户库。 */
  @Bean
  JwtDecoder jwtDecoder(@Value("${dup.auth.jwt-secret:DigitalUnionPayJwtSecretKey32B!!}") String secret) {
    return AccessJwt.decoder(secret);
  }

  /** 服务签名。打开时出站请求也会带上同一套头。 */
  @Bean
  ServiceCall serviceCall(
      JdbcTemplate jdbc,
      Clock utcClock,
      @Value("${dup.service-auth.secret:DigitalUnionPayServiceSecretKey32}") String secret,
      @Value("${dup.service-auth.enabled:true}") boolean enabled) {
    ServiceCall calls = new ServiceCall(secret, utcClock, new NonceStore(jdbc, utcClock));
    if (enabled) {
      ServiceSigner.install(calls);
    }
    return calls;
  }

  /** 本地消息表。业务事务里只调用 append。 */
  @Bean
  Outbox outbox(JdbcTemplate jdbc, Clock utcClock) {
    return new Outbox(jdbc, utcClock);
  }

  /** 投递失败后停放的死信。 */
  @Bean
  DeadLetters deadLetters(JdbcTemplate jdbc, Clock utcClock) {
    return new DeadLetters(jdbc, utcClock);
  }

  /** TCC 空回滚和悬挂判断。 */
  @Bean
  TccFence tccFence(JdbcTemplate jdbc, Clock utcClock) {
    return new TccFence(jdbc, utcClock);
  }

  /** 测试剖面关掉定时投递，避免去连 RocketMQ。 */
  @Bean
  @ConditionalOnProperty(name = "dup.outbox.publish", havingValue = "true", matchIfMissing = true)
  OutboxRelay outboxRelay(
      JdbcTemplate jdbc, DomainEvents events, DeadLetters deadLetters, Clock utcClock, PlatformMeters meters) {
    return new OutboxRelay(jdbc, events, deadLetters, utcClock, meters);
  }

  /** 入站 QPS 和发往 user-service 的 QPS。Sentinel 关闭时规则不装。 */
  @Bean
  SentinelRules sentinelRules(
      @Value("${dup.sentinel.inbound-qps:200}") double inboundQps,
      @Value("${dup.sentinel.user-service-qps:100}") double userServiceQps) {
    return new SentinelRules(inboundQps, userServiceQps);
  }

  /** 本地消息积压。Prometheus 抓 dup_outbox_backlog。 */
  @Bean
  PlatformMeters platformMeters(ObjectProvider<MeterRegistry> registries, JdbcTemplate jdbc) {
    MeterRegistry registry = registries.getIfAvailable();
    if (registry != null) {
      Gauge.builder(PlatformMeters.OUTBOX_BACKLOG, jdbc, PlatformMeters::backlog).register(registry);
    }
    return new PlatformMeters(registry);
  }

  /** 资金动作审计。调用方要放在业务事务里面。 */
  @Bean
  AuditLog auditLog(JdbcTemplate jdbc, Clock utcClock) {
    return new AuditLog(jdbc, utcClock);
  }

  /** 消费去重。 */
  @Bean
  Inbox inbox(JdbcTemplate jdbc, Clock utcClock) {
    return new Inbox(jdbc, utcClock);
  }

  /** 配置里的 32 字节数据密钥。这不是 HSM。 */
  @Bean
  FieldCipher fieldCipher(@Value("${dup.crypto.key:DigitalUnionPayAes256DataKey!!!!}") String key) {
    return new FieldCipher(key.getBytes(StandardCharsets.UTF_8));
  }

  /** 只有显式声明消费者时才订阅。缺类型或版本过高进死信。同一幂等键只处理一次。 */
  @Bean
  @ConditionalOnProperty(name = "spring.cloud.function.definition", havingValue = "domainEvents")
  Consumer<Message<String>> domainEvents(DeadLetters deadLetters, Inbox inbox) {
    return message -> {
      Object type = message.getHeaders().get("eventType");
      String typeText = type == null ? "" : type.toString();
      if (typeText.isBlank()) {
        deadLetters.park("domainEvents", String.valueOf(message.getPayload()), "type");
        throw new IllegalArgumentException("type");
      }
      Object versionHeader = message.getHeaders().get("eventVersion");
      int version = versionHeader instanceof Number number ? number.intValue() : DomainEvents.EVENT_VERSION;
      if (version > DomainEvents.EVENT_VERSION) {
        deadLetters.park(typeText, String.valueOf(message.getPayload()), "version");
        throw new IllegalArgumentException("version");
      }
      Object key = message.getHeaders().get(RequestHeaders.IDEMPOTENCY_KEY);
      if (key != null && !key.toString().isBlank() && !inbox.first(key.toString(), typeText, version)) {
        return;
      }
    };
  }

  /** 按 seata.enabled 和 spring.cloud.sentinel.enabled 记下运行开关。 */
  @Bean
  AlibabaSwitches alibabaSwitches(
      @Value("${seata.enabled:true}") boolean seataEnabled,
      @Value("${spring.cloud.sentinel.enabled:true}") boolean sentinelEnabled) {
    return AlibabaSwitches.apply(seataEnabled, sentinelEnabled);
  }
}
