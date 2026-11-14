package com.digitalunionpay.physical.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.physical.PhysicalIntakePort;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.valuation.ValuationPort;

public final class SkeletonPhysicalIntakePort implements PhysicalIntakePort {

  private final ValuationPort valuation;
  private final TccLedger ledger;

  public SkeletonPhysicalIntakePort(ValuationPort valuation, TccLedger ledger) {
    if (valuation == null || ledger == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.valuation = valuation;
    this.ledger = ledger;
  }

  @Override
  public String record(String deviceId, String rfid, Money valuationAmount) {
    GlobalTransactions.run("physical.record", () -> recordIntake(deviceId, rfid, valuationAmount));
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "PhysicalIntakePort.record");
  }

  private void recordIntake(String deviceId, String rfid, Money valuationAmount) {
    Ids.required(deviceId, "deviceId");
    Ids.required(rfid, "rfid");
    if (valuationAmount == null) {
      throw new IllegalArgumentException("valuation");
    }
    if (valuation == null || ledger == null) {
      throw new IllegalStateException("collaborators");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "PhysicalIntakePort.record");
  }
}
