package com.digitalunionpay.crypto.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.crypto.mapper.SignatureMapper;
import com.digitalunionpay.crypto.service.CustodySignService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class CustodySignServiceImpl implements CustodySignService {

  private final SignatureMapper signatures;

  /** 接入协作者和本聚合的 Mapper。 */
  public CustodySignServiceImpl(SignatureMapper signatures) {
    this.signatures = signatures;
  }

  /** 校验载荷引用，业务尚未实现。 */
  @Override
  public String sign(String payloadRef) {
    Ids.required(payloadRef, "payloadRef");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "MpcSigner.sign");
  }
}
