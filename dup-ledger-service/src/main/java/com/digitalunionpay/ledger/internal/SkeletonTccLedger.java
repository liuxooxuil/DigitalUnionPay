package com.digitalunionpay.ledger.internal;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.FreezeId;
import com.digitalunionpay.ledger.FreezeRequest;
import com.digitalunionpay.ledger.JournalCommand;
import com.digitalunionpay.ledger.TccLedger;

public final class SkeletonTccLedger implements TccLedger {

  @Override
  public FreezeId tryFreeze(FreezeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TccLedger.tryFreeze");
  }

  @Override
  public void confirm(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TccLedger.confirm");
  }

  @Override
  public void cancel(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TccLedger.cancel");
  }

  @Override
  public void post(JournalCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    command.requireBalanced();
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "TccLedger.post");
  }
}
