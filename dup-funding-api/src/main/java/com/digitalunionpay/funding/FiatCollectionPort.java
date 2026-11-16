package com.digitalunionpay.funding;

/** 法币入金渠道。虚拟 IBAN 由 Airwallex 一类的渠道适配器实现。 */
public interface FiatCollectionPort {

  /** 为用户签发指定币种的虚拟 IBAN。 */
  String issueVirtualIban(String userId, String currency);
}
