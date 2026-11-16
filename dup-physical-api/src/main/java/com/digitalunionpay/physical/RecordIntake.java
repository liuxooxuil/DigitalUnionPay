package com.digitalunionpay.physical;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 实物入金登记入参，含设备、RFID 和估值。 */
public record RecordIntake(String deviceId, String rfid, Money valuation) {

  /** 拒绝空白的设备号和 RFID，以及空的估值。 */
  public RecordIntake {
    deviceId = Ids.required(deviceId, "deviceId");
    rfid = Ids.required(rfid, "rfid");
    if (valuation == null) {
      throw new IllegalArgumentException("valuation");
    }
  }
}
