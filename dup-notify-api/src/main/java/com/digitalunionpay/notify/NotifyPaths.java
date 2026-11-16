package com.digitalunionpay.notify;

/** 通知服务对外的内部 HTTP 路径。 */
public final class NotifyPaths {
  /** 提交一条通知。 */
  public static final String NOTIFICATIONS = "/internal/notifications";

  /** 禁止实例化。 */
  private NotifyPaths() {
  }
}
