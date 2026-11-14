package com.digitalunionpay.funding.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.funding.FiatCollectionPort;

public final class SkeletonFiatCollectionPort implements FiatCollectionPort {

  @Override
  public String issueVirtualIban(String userId, String currency) {
    Ids.required(userId, "userId");
    Ids.required(currency, "currency");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "FiatCollectionPort.issueVirtualIban");
  }
}
