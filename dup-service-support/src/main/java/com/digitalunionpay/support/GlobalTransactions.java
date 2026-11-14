package com.digitalunionpay.support;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;

/** 跨服务写操作的 Seata 全局事务。Seata 关闭时直接执行业务体。 */
public final class GlobalTransactions {

  private GlobalTransactions() {
  }

  public static void run(String name, Runnable action) {
    if (!AlibabaSwitches.seata()) {
      action.run();
      return;
    }
    GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();
    try {
      transaction.begin(60_000, name);
      action.run();
      transaction.commit();
    } catch (RuntimeException ex) {
      rollback(transaction);
      throw ex;
    } catch (Exception ex) {
      rollback(transaction);
      throw new IllegalStateException(name, ex);
    }
  }

  private static void rollback(GlobalTransaction transaction) {
    try {
      transaction.rollback();
    } catch (TransactionException ignored) {
      // 开始事务失败时没有可回滚的全局事务。
    }
  }
}
