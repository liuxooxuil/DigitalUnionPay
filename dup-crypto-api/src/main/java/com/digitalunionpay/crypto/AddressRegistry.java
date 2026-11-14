package com.digitalunionpay.crypto;

public interface AddressRegistry {

  String depositAddress(String userId, String chain);
}
