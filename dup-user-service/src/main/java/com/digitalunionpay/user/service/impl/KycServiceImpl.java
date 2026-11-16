package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.mapper.UserProfileMapper;
import com.digitalunionpay.user.service.KycService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class KycServiceImpl implements KycService {

  private final UserProfileMapper userProfileMapper;

  /** 接入本聚合的 Mapper。 */
  public KycServiceImpl(UserProfileMapper userProfileMapper) {
    this.userProfileMapper = userProfileMapper;
  }

  /** 校验用户号后拒绝查询。 */
  @Override
  public boolean fundingAllowed(String userId) {
    Ids.required(userId, "userId");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "KycGate.fundingAllowed");
  }
}
