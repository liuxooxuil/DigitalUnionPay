package com.digitalunionpay.ledger;

/** 清结算服务对外的内部 HTTP 路径。 */
public final class LedgerPaths {
  /** 预冻结。 */
  public static final String FREEZES = "/internal/ledger/freezes";
  /** 确认预冻结。 */
  public static final String FREEZE_CONFIRM = "/internal/ledger/freezes/{id}/confirm";
  /** 回滚预冻结。 */
  public static final String FREEZE_CANCEL = "/internal/ledger/freezes/{id}/cancel";
  /** 提交复式分录。 */
  public static final String JOURNALS = "/internal/ledger/journals";

  /** 禁止实例化。 */
  private LedgerPaths() {
  }
}
