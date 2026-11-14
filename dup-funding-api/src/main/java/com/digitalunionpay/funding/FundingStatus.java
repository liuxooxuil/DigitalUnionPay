package com.digitalunionpay.funding;

/** 待确认 → 已到账 → 已入账 / 异常 / 退款。 */
public enum FundingStatus {
  PENDING_CONFIRM,
  RECEIVED,
  POSTED,
  EXCEPTION,
  REFUND
}
