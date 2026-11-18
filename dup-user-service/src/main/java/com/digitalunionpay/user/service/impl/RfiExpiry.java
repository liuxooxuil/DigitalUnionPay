package com.digitalunionpay.user.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 把过期的补充材料单独提交，避免外层拒绝把这次更新回滚掉。 */
@Component
public class RfiExpiry {

  private final JdbcTemplate jdbc;

  /** 接入案件表。 */
  public RfiExpiry(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** 案件和档案都改成拒绝，原因是 rfi-expired。 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void expire(long caseId, String userId, Instant now) {
    Timestamp stamp = Timestamp.from(now);
    jdbc.update(
        "UPDATE kyc_case SET status = 'REJECTED', reject_reason = 'rfi-expired', decided_at = ? WHERE id = ?",
        stamp,
        caseId);
    jdbc.update("UPDATE user_profile SET kyc_status = 'REJECTED' WHERE user_id = ?", userId);
  }
}
