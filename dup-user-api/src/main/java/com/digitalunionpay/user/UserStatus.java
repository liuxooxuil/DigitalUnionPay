package com.digitalunionpay.user;

/** 用户从待 KYC 到激活、冻结、关闭或已继承。 */
public enum UserStatus {
  /** 尚未完成 KYC。 */
  PENDING_KYC,
  /** 已激活。 */
  ACTIVE,
  /** 已冻结。 */
  FROZEN,
  /** 已关闭。 */
  CLOSED,
  /** 已完成继承。 */
  INHERITED
}
