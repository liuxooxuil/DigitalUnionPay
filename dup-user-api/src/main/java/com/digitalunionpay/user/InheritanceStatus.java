package com.digitalunionpay.user;

/** PENDING → UNDER_REVIEW → FROZEN → COMPLETED / REJECTED。 */
public enum InheritanceStatus {
  /** 待受理。 */
  PENDING,
  /** 审核中。 */
  UNDER_REVIEW,
  /** 案件冻结中。 */
  FROZEN,
  /** 已完成。 */
  COMPLETED,
  /** 已拒绝。 */
  REJECTED
}
