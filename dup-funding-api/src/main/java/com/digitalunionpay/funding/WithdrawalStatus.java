package com.digitalunionpay.funding;

/** 申请中 → 审核中 → 处理中 → 成功 / 失败。 */
public enum WithdrawalStatus {
  APPLIED,
  IN_REVIEW,
  PROCESSING,
  SUCCEEDED,
  FAILED
}
