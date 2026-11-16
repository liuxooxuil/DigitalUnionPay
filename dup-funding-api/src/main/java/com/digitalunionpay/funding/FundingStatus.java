package com.digitalunionpay.funding;

/** 待确认 → 已到账 → 已入账 / 异常 / 退款。 */
public enum FundingStatus {
  /** 待确认。 */
  PENDING_CONFIRM,
  /** 已到账。 */
  RECEIVED,
  /** 已入账。 */
  POSTED,
  /** 异常。 */
  EXCEPTION,
  /** 退款。 */
  REFUND
}
