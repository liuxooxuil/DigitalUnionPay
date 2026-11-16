package com.digitalunionpay.ledger.service.impl;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.support.PlatformMeters;
import org.springframework.jdbc.core.JdbcTemplate;

/** 同一账户的可用和冻结余额。没有账户行时不动余额，已开户的扣减不能超额。 */
final class AccountBalances {

  private final JdbcTemplate jdbc;

  private final PlatformMeters meters;

  /** 保存后续更新要用的连接和慢调用计数。 */
  AccountBalances(JdbcTemplate jdbc, PlatformMeters meters) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.meters = meters;
  }

  /** 借方减少可用余额，贷方增加可用余额。 */
  void apply(String accountId, String currency, int scale, boolean debit, long minor) {
    move(accountId, currency, scale, debit ? -minor : minor, 0);
  }

  /** 预冻结：可用转冻结。 */
  void reserve(String accountId, String currency, int scale, long minor) {
    move(accountId, currency, scale, -minor, minor);
  }

  /** 取消冻结：冻结转回可用。 */
  void release(String accountId, String currency, int scale, long minor) {
    move(accountId, currency, scale, minor, -minor);
  }

  /** 确认冻结：冻结被划走，不再回到可用。 */
  void capture(String accountId, String currency, int scale, long minor) {
    move(accountId, currency, scale, 0, -minor);
  }

  /** 归属人。没有账户行时返回 null。 */
  String owner(String accountId, String currency) {
    return jdbc.query(
        "SELECT owner_user_id FROM ledger_account WHERE account_id = ? AND currency = ?",
        rs -> rs.next() ? rs.getString(1) : null,
        accountId,
        currency);
  }

  /** 记一次余额更新耗时。超过 200 毫秒记入 dup.db.slow。 */
  private void move(String accountId, String currency, int scale, long availableDelta, long frozenDelta) {
    long started = System.nanoTime();
    try {
      moveOnce(accountId, currency, scale, availableDelta, frozenDelta);
    } finally {
      if (meters != null) {
        meters.slow((System.nanoTime() - started) / 1_000_000L, 200L);
      }
    }
  }

  /** 乐观锁重试。版本抢失败就再读一次。 */
  private void moveOnce(String accountId, String currency, int scale, long availableDelta, long frozenDelta) {
    for (int attempt = 0; attempt < 5; attempt++) {
      Row row = find(accountId, currency);
      if (row == null) {
        return;
      }
      if (row.scale != scale) {
        throw new BusinessException(ErrorCode.UNBALANCED_JOURNAL, currency);
      }
      long available = add(currency, row.available, availableDelta);
      long frozen = add(currency, row.frozen, frozenDelta);
      if (available < 0) {
        throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, accountId);
      }
      if (frozen < 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "freeze");
      }
      int updated = jdbc.update(
          "UPDATE ledger_account SET available = ?, frozen = ?, version = version + 1 "
              + "WHERE account_id = ? AND currency = ? AND version = ?",
          available,
          frozen,
          accountId,
          currency,
          row.version);
      if (updated == 1) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.CONFLICT, "version");
  }

  /** 没有记录时返回 null。 */
  private Row find(String accountId, String currency) {
    return jdbc.query(
        "SELECT available, frozen, scale, version FROM ledger_account WHERE account_id = ? AND currency = ?",
        rs -> rs.next() ? new Row(rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getInt(4)) : null,
        accountId,
        currency);
  }

  /** 溢出按不平衡拒绝。 */
  private static long add(String currency, long left, long right) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException exception) {
      throw new BusinessException(ErrorCode.UNBALANCED_JOURNAL, currency);
    }
  }

  /** 读到的一行余额。 */
  private record Row(long available, long frozen, int scale, int version) {
  }
}
