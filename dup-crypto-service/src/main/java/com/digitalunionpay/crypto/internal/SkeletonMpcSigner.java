package com.digitalunionpay.crypto.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.crypto.MpcSigner;

public final class SkeletonMpcSigner implements MpcSigner {

  @Override
  public String sign(String payloadRef) {
    Ids.required(payloadRef, "payloadRef");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "MpcSigner.sign");
  }
}
