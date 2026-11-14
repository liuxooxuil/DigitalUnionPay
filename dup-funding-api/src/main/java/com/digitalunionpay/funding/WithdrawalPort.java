package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;

public interface WithdrawalPort {

  String submit(String userId, Money amount, String beneficiaryRef);
}
