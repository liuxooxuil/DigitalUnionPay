package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;

/** 出金申请。只提交申请，不在这里改余额。 */
public interface WithdrawalPort {

  /** 提交一笔出金并返回申请号。 */
  String submit(String userId, Money amount, String beneficiaryRef);
}
