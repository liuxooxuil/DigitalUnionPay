package com.digitalunionpay.crypto.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.crypto.mapper.DepositAddressMapper;
import com.digitalunionpay.crypto.service.DepositAddressService;
import com.digitalunionpay.ledger.TccLedger;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class DepositAddressServiceImpl implements DepositAddressService {

  private final TccLedger ledger;
  private final DepositAddressMapper depositAddresses;

  /** 接入协作者和本聚合的 Mapper。 */
  public DepositAddressServiceImpl(TccLedger ledger, DepositAddressMapper depositAddresses) {
    if (ledger == null) {
      throw new IllegalArgumentException("ledger");
    }
    this.ledger = ledger;
    this.depositAddresses = depositAddresses;
  }

  /** 校验用户、链和清结算协作者，业务尚未实现。 */
  @Override
  public String depositAddress(String userId, String chain) {
    Ids.required(userId, "userId");
    Ids.required(chain, "chain");
    if (ledger == null) {
      throw new IllegalStateException("ledger");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "AddressRegistry.depositAddress");
  }
}
