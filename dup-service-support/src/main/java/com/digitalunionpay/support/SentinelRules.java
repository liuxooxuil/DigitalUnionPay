package com.digitalunionpay.support;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.digitalunionpay.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/** 入站和出站的具体 QPS 规则。Sentinel 关闭时过滤器和规则都不生效。 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
final class SentinelRules extends OncePerRequestFilter implements ApplicationRunner {

  /** 入站统一资源名。 */
  static final String INBOUND = "inbound";

  private final double inboundQps;

  private final double userServiceQps;

  /** 保存两条规则的阈值。 */
  SentinelRules(double inboundQps, double userServiceQps) {
    this.inboundQps = inboundQps;
    this.userServiceQps = userServiceQps;
  }

  /** 进程启动时装上 QPS 规则。出站资源名是 user-service。 */
  @Override
  public void run(ApplicationArguments args) {
    if (!AlibabaSwitches.sentinel()) {
      return;
    }
    FlowRule inbound = rule(INBOUND, inboundQps);
    FlowRule user = rule("user-service", userServiceQps);
    FlowRuleManager.loadRules(List.of(inbound, user));
    DegradeRule degrade = new DegradeRule("user-service");
    degrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
    degrade.setCount(0.5);
    degrade.setTimeWindow(10);
    degrade.setMinRequestAmount(5);
    DegradeRuleManager.loadRules(List.of(degrade));
  }

  /** 入站请求申请 inbound 资源。被限流时返回 503。 */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!AlibabaSwitches.sentinel()) {
      chain.doFilter(request, response);
      return;
    }
    Entry entry = null;
    try {
      entry = SphU.entry(INBOUND);
      chain.doFilter(request, response);
    } catch (BlockException exception) {
      ErrorResponses.write(response, ErrorCode.UNAVAILABLE, INBOUND);
    } finally {
      if (entry != null) {
        entry.exit();
      }
    }
  }

  /** 一条 QPS 限流规则。 */
  private static FlowRule rule(String resource, double count) {
    FlowRule rule = new FlowRule();
    rule.setResource(resource);
    rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
    rule.setCount(count);
    return rule;
  }
}
