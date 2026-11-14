package com.digitalunionpay.ledger;

/**
 * 清结算事务边界。Try 预冻结，Confirm 入账，Cancel 回滚。
 * post 只接受已经借贷平衡的分录。
 */
public interface TccLedger {

  FreezeId tryFreeze(FreezeRequest request);

  void confirm(FreezeId freezeId);

  void cancel(FreezeId freezeId);

  void post(JournalCommand command);
}
