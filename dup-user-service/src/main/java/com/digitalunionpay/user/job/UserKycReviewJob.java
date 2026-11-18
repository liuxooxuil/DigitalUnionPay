package com.digitalunionpay.user.job;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天把超过 7 天还没补材料的 KYC 改成拒绝。 */
@Component
public class UserKycReviewJob {

  private final JdbcTemplate jdbc;

  private final Clock clock;

  /** 接入案件表。 */
  public UserKycReviewJob(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /** 只处理补充材料已经过期、且用户没有更新的待补案件。 */
  @Scheduled(cron = "0 0 3 * * ?")
  public void review() {
    Timestamp now = Timestamp.from(clock.instant());
    jdbc.update(
        """
        UPDATE kyc_case
        SET status = 'REJECTED', reject_reason = 'rfi-expired', decided_at = ?
        WHERE status = 'RFI' AND rfi_due_at IS NOT NULL AND rfi_due_at < ?
        """,
        now,
        now);
    jdbc.update(
        """
        UPDATE user_profile
        SET kyc_status = 'REJECTED'
        WHERE kyc_status = 'RFI'
          AND user_id NOT IN (SELECT user_id FROM kyc_case WHERE status = 'RFI')
        """);
  }
}
