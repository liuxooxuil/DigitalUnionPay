package com.digitalunionpay.physical;

import com.digitalunionpay.common.money.Money;

public interface PhysicalIntakePort {

  String record(String deviceId, String rfid, Money valuation);
}
