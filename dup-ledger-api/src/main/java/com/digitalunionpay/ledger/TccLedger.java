package com.digitalunionpay.ledger;

/**
 * 清结算事务边界。Try 预冻结，Confirm 入账，Cancel 回滚。
 * post 只接受已经借贷平衡的分录。
 */
public interface TccLedger {

  /** 预冻结并返回冻结编号。 */
  FreezeId tryFreeze(FreezeRequest request);

  /** 确认这笔预冻结并入账。 */
  void confirm(FreezeId freezeId);

  /** 回滚这笔预冻结。 */
  void cancel(FreezeId freezeId);

  /** 直接提交一笔已平衡的分录。 */
  void post(JournalCommand command);
}
