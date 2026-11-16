package com.digitalunionpay.risk.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.risk.RiskAction;
import com.digitalunionpay.risk.RiskDecision;
import com.digitalunionpay.risk.mapper.RiskCheckMapper;
import com.digitalunionpay.risk.service.RiskCheckService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class RiskCheckServiceImpl implements RiskCheckService {

  private final RiskCheckMapper riskCheckMapper;

  /** 接入本聚合的 Mapper。 */
  public RiskCheckServiceImpl(RiskCheckMapper riskCheckMapper) {
    this.riskCheckMapper = riskCheckMapper;
  }

  /** 校验用户、动作和金额后拒绝给出结论。 */
  @Override
  public RiskDecision check(String userId, RiskAction action, Money amount) {
    Ids.required(userId, "userId");
    if (action == null || amount == null) {
      throw new IllegalArgumentException("action and amount");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "RiskGate.check");
  }
}
