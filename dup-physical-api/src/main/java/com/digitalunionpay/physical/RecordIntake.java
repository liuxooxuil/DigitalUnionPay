package com.digitalunionpay.physical;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record RecordIntake(String deviceId, String rfid, Money valuation) {

  public RecordIntake {
    deviceId = Ids.required(deviceId, "deviceId");
    rfid = Ids.required(rfid, "rfid");
    if (valuation == null) {
      throw new IllegalArgumentException("valuation");
    }
  }
}
