package com.digitalunionpay.user;

/** 未提交 → 审核中 → 通过 / 拒绝 / 补充材料。 */
public enum KycStatus {
  NOT_SUBMITTED,
  IN_REVIEW,
  APPROVED,
  REJECTED,
  RFI;

  public boolean allowsFunding() {
    return this == APPROVED;
  }
}
