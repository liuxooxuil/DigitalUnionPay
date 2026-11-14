package com.digitalunionpay.funding.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.funding.WithdrawalPort;

public final class SkeletonWithdrawalPort implements WithdrawalPort {

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
