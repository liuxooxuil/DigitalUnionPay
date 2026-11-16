package com.digitalunionpay.user;

/** 账户是正常、冻结还是关闭。 */
public enum AccountStatus {
  /** 可正常使用。 */
  NORMAL,
  /** 已冻结。 */
  FROZEN,
  /** 已关闭。 */
  CLOSED
}
