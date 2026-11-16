package com.digitalunionpay.risk.controller;

import com.digitalunionpay.risk.RiskCheck;
import com.digitalunionpay.risk.RiskDecision;
import com.digitalunionpay.risk.RiskPaths;
import com.digitalunionpay.risk.service.RiskCheckService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class RiskController {

  private final RiskCheckService riskGate;

  /** 接入本进程的服务入口。 */
  public RiskController(RiskCheckService riskGate) {
    this.riskGate = riskGate;
  }

  /** 受理一次同步风控检查。 */
  @PostMapping(RiskPaths.CHECKS)
  public RiskDecision check(@RequestBody RiskCheck request) {
    return riskGate.check(request.userId(), request.action(), request.amount());
  }
}
