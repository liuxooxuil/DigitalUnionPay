package com.digitalunionpay.crypto;

/** 充值地址查询。只返回地址，不改客户余额。 */
public interface AddressRegistry {

  /** 按用户和链返回充值地址。 */
  String depositAddress(String userId, String chain);
}
