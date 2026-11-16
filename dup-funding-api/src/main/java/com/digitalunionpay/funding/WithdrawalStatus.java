package com.digitalunionpay.funding;

/** 申请中 → 审核中 → 处理中 → 成功 / 失败。 */
public enum WithdrawalStatus {
  /** 申请中。 */
  APPLIED,
  /** 审核中。 */
  IN_REVIEW,
  /** 处理中。 */
  PROCESSING,
  /** 成功。 */
  SUCCEEDED,
  /** 失败。 */
  FAILED
}
