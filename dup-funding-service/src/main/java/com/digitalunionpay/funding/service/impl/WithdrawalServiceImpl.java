package com.digitalunionpay.funding.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.funding.mapper.WithdrawalMapper;
import com.digitalunionpay.funding.service.WithdrawalService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class WithdrawalServiceImpl implements WithdrawalService {

  private final WithdrawalMapper withdrawalMapper;

  /** 接入协作者和本聚合的 Mapper。 */
  public WithdrawalServiceImpl(WithdrawalMapper withdrawalMapper) {
    this.withdrawalMapper = withdrawalMapper;
  }

  /** 校验用户、收款人引用和金额后抛出 PORT_NOT_IMPLEMENTED。 */
  @Override
  public String submit(String userId, Money amount, String beneficiaryRef) {
    Ids.required(userId, "userId");
    Ids.required(beneficiaryRef, "beneficiaryRef");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "WithdrawalPort.submit");
  }
}
