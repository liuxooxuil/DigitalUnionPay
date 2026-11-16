package com.digitalunionpay.funding.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.funding.mapper.VirtualIbanMapper;
import com.digitalunionpay.funding.service.FiatCollectionService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class FiatCollectionServiceImpl implements FiatCollectionService {

  private final VirtualIbanMapper virtualIbanMapper;

  /** 接入协作者和本聚合的 Mapper。 */
  public FiatCollectionServiceImpl(VirtualIbanMapper virtualIbanMapper) {
    this.virtualIbanMapper = virtualIbanMapper;
  }

  /** 校验用户和币种后抛出 PORT_NOT_IMPLEMENTED。 */
  @Override
  public String issueVirtualIban(String userId, String currency) {
    Ids.required(userId, "userId");
    Ids.required(currency, "currency");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "FiatCollectionPort.issueVirtualIban");
  }
}
