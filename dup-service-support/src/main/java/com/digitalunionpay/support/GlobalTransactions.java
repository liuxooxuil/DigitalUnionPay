package com.digitalunionpay.support;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;

/** 跨服务写操作的 Seata 全局事务。Seata 关闭时直接执行业务体。 */
public final class GlobalTransactions {

  /** 禁止实例化。 */
  private GlobalTransactions() {
  }

  /** 在超时 60 秒的全局事务里执行 action。失败时回滚并继续抛出原异常。 */
  public static void run(String name, Runnable action) {
    run(name, 60_000, action);
  }

  /** 超时由调用方指定。Seata 关闭时直接执行，不在这里重试业务体。 */
  public static void run(String name, int timeoutMillis, Runnable action) {
    if (action == null) {
      throw new IllegalArgumentException("action");
    }
    if (timeoutMillis < 1) {
      throw new IllegalArgumentException("timeoutMillis");
    }
    if (!AlibabaSwitches.seata()) {
      action.run();
      return;
    }
    GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();
    try {
      transaction.begin(timeoutMillis, name);
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

  /** 回滚当前全局事务。事务尚未开始时忽略回滚失败。 */
  private static void rollback(GlobalTransaction transaction) {
    try {
      transaction.rollback();
    } catch (TransactionException ignored) {
      // 开始事务失败时没有可回滚的全局事务。
    }
  }
}
