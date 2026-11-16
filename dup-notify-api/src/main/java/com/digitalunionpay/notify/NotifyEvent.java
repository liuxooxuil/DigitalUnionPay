package com.digitalunionpay.notify;

/** 业务模块可以提交的通知事件。 */
public enum NotifyEvent {
  /** 入金已入账。 */
  DEPOSIT_POSTED,
  /** 兑换结果。 */
  EXCHANGE_RESULT,
  /** 出金结果。 */
  WITHDRAWAL_RESULT,
  /** 被风控拦截。 */
  RISK_BLOCKED,
  /** KYC 结果。 */
  KYC_RESULT,
  /** 继承进度。 */
  INHERITANCE_PROGRESS,
  /** 保证金追缴。 */
  MARGIN_CALL
}
