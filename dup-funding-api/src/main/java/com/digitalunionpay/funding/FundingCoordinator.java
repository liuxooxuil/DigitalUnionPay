package com.digitalunionpay.funding;

import com.digitalunionpay.common.money.Money;

/**
 * 法币到账后的应用入口。顺序是 KYC、风控，再进入清结算 TCC。
 * 兑换不在本模块里调用。
 */
public interface FundingCoordinator {

  void onFiatSettled(String userId, Money amount, String providerReference);
}
