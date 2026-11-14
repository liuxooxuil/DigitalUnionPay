package com.digitalunionpay.funding;

/** 法币入金渠道。虚拟 IBAN 由 Airwallex 一类的渠道适配器实现。 */
public interface FiatCollectionPort {

  String issueVirtualIban(String userId, String currency);
}
