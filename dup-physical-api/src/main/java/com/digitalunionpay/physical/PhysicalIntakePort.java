package com.digitalunionpay.physical;

import com.digitalunionpay.common.money.Money;

/** 登记实物入金。估值后的记账交给清结算，不在这里改余额。 */
public interface PhysicalIntakePort {

  /** 按设备、RFID 和估值登记一次入金，返回入金号。 */
  String record(String deviceId, String rfid, Money valuation);
}
