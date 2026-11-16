package com.digitalunionpay.support;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** 各进程默认优雅停机，并带上统一的链路号日志。应用自己的配置优先。 */
public final class BaseDefaults implements EnvironmentPostProcessor {

  /** 最低优先级写入默认值，yml 里的同名项会盖过这里。 */
  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> values = new HashMap<>();
    values.put("server.shutdown", "graceful");
    values.put("spring.lifecycle.timeout-per-shutdown-phase", "20s");
    values.put("logging.pattern.level", "%5p [trace:%X{X-Trace-Id:-}]");
    values.put("spring.flyway.baseline-on-migrate", "true");
    values.put("spring.flyway.baseline-version", "0");
    values.put("spring.sql.init.mode", "never");
    values.put("management.tracing.sampling.probability", "1.0");
    String otlp = System.getenv("OTLP_ENDPOINT");
    if (otlp == null || otlp.isBlank()) {
      values.put("management.otlp.tracing.export.enabled", "false");
    } else {
      values.put("management.otlp.tracing.endpoint", otlp);
      values.put("management.otlp.tracing.export.enabled", "true");
    }
    String applicationName = environment.getProperty("spring.application.name");
    if (applicationName != null && !applicationName.isBlank()) {
      values.put("spring.flyway.table", "flyway_" + applicationName.replace('-', '_'));
    }
    environment.getPropertySources().addLast(new MapPropertySource("digitalunionpay-base", values));
  }
}
