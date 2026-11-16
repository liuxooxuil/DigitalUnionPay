package com.digitalunionpay.user;

/** 未提交 → 审核中 → 通过 / 拒绝 / 补充材料。 */
public enum KycStatus {
  /** 尚未提交。 */
  NOT_SUBMITTED,
  /** 审核中。 */
  IN_REVIEW,
  /** 已通过。 */
  APPROVED,
  /** 已拒绝。 */
  REJECTED,
  /** 需要补充材料。 */
  RFI;

  /** 只有已通过才返回 true。 */
  public boolean allowsFunding() {
    return this == APPROVED;
  }
}
