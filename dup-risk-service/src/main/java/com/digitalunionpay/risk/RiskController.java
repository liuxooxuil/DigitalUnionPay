package com.digitalunionpay.risk;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiskController {

  private final RiskGate riskGate;

  public RiskController(RiskGate riskGate) {
    this.riskGate = riskGate;
  }

  @PostMapping(RiskPaths.CHECKS)
  public RiskDecision check(@RequestBody RiskCheck request) {
    return riskGate.check(request.userId(), request.action(), request.amount());
  }
}
