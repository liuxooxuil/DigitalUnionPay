package com.digitalunionpay.crypto.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.crypto.AddressRegistry;
import com.digitalunionpay.ledger.TccLedger;

public final class SkeletonAddressRegistry implements AddressRegistry {

  private final TccLedger ledger;

  public SkeletonAddressRegistry(TccLedger ledger) {
    if (ledger == null) {
      throw new IllegalArgumentException("ledger");
    }
    this.ledger = ledger;
  }

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
