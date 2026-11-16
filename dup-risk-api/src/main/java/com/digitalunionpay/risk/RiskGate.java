package com.digitalunionpay.risk;

import com.digitalunionpay.common.money.Money;

/** 入金、出金、兑换、交易前的同步拦截。 */
public interface RiskGate {

  /** 入账前按动作和金额做同步拦截。 */
  RiskDecision check(String userId, RiskAction action, Money amount);
}
