package com.digitalunionpay.support;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** TCC 事务栅栏。Cancel 先到记空回滚，之后的 Try 按悬挂拒绝。 */
public final class TccFence {

  private static final String TRIED = "TRIED";

  private static final String CONFIRMED = "CONFIRMED";

  private static final String CANCELLED = "CANCELLED";

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 保存后续更新要用的时钟。 */
  public TccFence(JdbcTemplate jdbc, Clock clock) {
    if (jdbc == null) {
      throw new IllegalArgumentException("jdbc");
    }
    this.jdbc = jdbc;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** 开始 Try。已经空回滚过的分支按悬挂拒绝。重复 Try 直接返回。 */
  public void tryBegin(String branchId) {
    Ids.required(branchId, "branchId");
    try {
      insert(branchId, TRIED);
      return;
    } catch (DuplicateKeyException exception) {
      String status = status(branchId);
      if (CANCELLED.equals(status)) {
        throw new BusinessException(ErrorCode.CONFLICT, "hanging");
      }
    }
  }

  /** 只有 Try 过的分支能确认。重复确认直接返回。 */
  public void confirm(String branchId) {
    Ids.required(branchId, "branchId");
    if (mark(branchId, TRIED, CONFIRMED) == 1) {
      return;
    }
    String status = status(branchId);
    if (CONFIRMED.equals(status)) {
      return;
    }
    if (CANCELLED.equals(status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "hanging");
    }
    throw new BusinessException(ErrorCode.CONFLICT, "missing");
  }

  /** 没有 Try 记录时写入空回滚并返回。已经取消的再次调用也返回。 */
  public void cancel(String branchId) {
    Ids.required(branchId, "branchId");
    if (mark(branchId, TRIED, CANCELLED) == 1) {
      return;
    }
    String status = status(branchId);
    if (status == null) {
      try {
        insert(branchId, CANCELLED);
      } catch (DuplicateKeyException exception) {
        cancel(branchId);
      }
      return;
    }
    if (CANCELLED.equals(status)) {
      return;
    }
    throw new BusinessException(ErrorCode.CONFLICT, "confirmed");
  }

  /** 插入一条栅栏。主键冲突交给调用方判断状态。 */
  private void insert(String branchId, String status) {
    jdbc.update(
        "INSERT INTO dup_tcc_fence (branch_id, status, updated_at) VALUES (?, ?, ?)",
        branchId,
        status,
        Timestamp.from(clock.instant()));
  }

  /** 只从指定状态改到目标状态。 */
  private int mark(String branchId, String from, String to) {
    return jdbc.update(
        "UPDATE dup_tcc_fence SET status = ?, updated_at = ? WHERE branch_id = ? AND status = ?",
        to,
        Timestamp.from(clock.instant()),
        branchId,
        from);
  }

  /** 没有记录时返回 null。 */
  private String status(String branchId) {
    return jdbc.query(
        "SELECT status FROM dup_tcc_fence WHERE branch_id = ?",
        rs -> rs.next() ? rs.getString(1) : null,
        branchId);
  }
}
