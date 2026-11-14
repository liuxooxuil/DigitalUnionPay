package com.digitalunionpay.user.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.KycGate;

public final class SkeletonKycGate implements KycGate {

  @Override
  public boolean fundingAllowed(String userId) {
    Ids.required(userId, "userId");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "KycGate.fundingAllowed");
  }
}
