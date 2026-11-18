package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.AccountBalance;
import com.digitalunionpay.user.auth.Digests;
import com.digitalunionpay.user.auth.LongIds;
import com.digitalunionpay.user.ledger.CustomerLedger;
import com.digitalunionpay.user.service.CustomerViews;
import com.digitalunionpay.user.service.InheritanceCaseService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 继承双人复核。资金迁移走账本的平衡分录，失败时冻结一起回滚。类不是 final，事务代理要生成子类。 */
@Service
public class InheritanceCaseServiceImpl implements InheritanceCaseService {

  private final JdbcTemplate jdbc;

  private final CustomerLedger ledger;

  private final Clock clock;

  /** 接入案件表和账本。 */
  public InheritanceCaseServiceImpl(JdbcTemplate jdbc, CustomerLedger ledger, Clock clock) {
    this.jdbc = jdbc;
    this.ledger = ledger;
    this.clock = clock;
  }

  /** 材料只存 SHA-256。原账户和继承人必须是两个已经存在的用户。 */
  @Override
  @Transactional
  public void open(String caseId, String estateUserId, String heirUserId, String applicantRole, String evidenceBase64) {
    String id = Ids.required(caseId, "caseId");
    String estate = Ids.required(estateUserId, "estateUserId");
    String heir = Ids.required(heirUserId, "heirUserId");
    if (estate.equals(heir)) {
      throw new BusinessException(ErrorCode.CONFLICT, "user");
    }
    String role = Ids.required(applicantRole, "applicantRole").trim().toUpperCase(Locale.ROOT);
    if (!"HEIR".equals(role) && !"AGENT".equals(role)) {
      throw new IllegalArgumentException("applicantRole");
    }
    byte[] evidence = decode(evidenceBase64);
    requireProfile(estate, "estateUserId");
    requireProfile(heir, "heirUserId");
    String estateStatus = jdbc.query(
        "SELECT user_status FROM user_profile WHERE user_id = ?",
        rs -> rs.next() ? rs.getString(1) : null,
        estate);
    if ("INHERITED".equals(estateStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "estate");
    }
    Integer active = jdbc.queryForObject(
        """
        SELECT COUNT(*) FROM inheritance_case
        WHERE estate_user_id = ? AND status IN ('PENDING', 'UNDER_REVIEW', 'FROZEN')
        """,
        Integer.class,
        estate);
    if (active != null && active > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "estate");
    }
    Instant now = clock.instant();
    try {
      jdbc.update(
          """
          INSERT INTO inheritance_case (
            id, case_id, estate_user_id, heir_user_id, applicant_role, status, evidence_sha256,
            first_reviewer, second_reviewer, opened_at, updated_at)
          VALUES (?, ?, ?, ?, ?, 'PENDING', ?, NULL, NULL, ?, ?)
          """,
          LongIds.next(),
          id,
          estate,
          heir,
          role,
          Digests.sha256(evidence),
          Timestamp.from(now),
          Timestamp.from(now));
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.CONFLICT, "caseId");
    }
  }

  /** 两个复核人必须不同。继承人 KYC 未通过时不能进入资金迁移。 */
  @Override
  @Transactional
  public CustomerViews.InheritanceView review(String caseId, String reviewerId, boolean approve, String reason) {
    String id = Ids.required(caseId, "caseId");
    String reviewer = Ids.required(reviewerId, "reviewerId");
    OpenCase row = load(id);
    if (row == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "caseId");
    }
    Instant now = clock.instant();
    if (!approve) {
      if (!"PENDING".equals(row.status) && !"UNDER_REVIEW".equals(row.status)) {
        throw new BusinessException(ErrorCode.CONFLICT, "status");
      }
      jdbc.update(
          "UPDATE inheritance_case SET status = 'REJECTED', updated_at = ? WHERE case_id = ?",
          Timestamp.from(now),
          id);
      return view(id, "REJECTED", row.estateUserId, row.heirUserId);
    }
    if ("PENDING".equals(row.status)) {
      jdbc.update(
          "UPDATE inheritance_case SET status = 'UNDER_REVIEW', first_reviewer = ?, updated_at = ? WHERE case_id = ?",
          reviewer,
          Timestamp.from(now),
          id);
      return view(id, "UNDER_REVIEW", row.estateUserId, row.heirUserId);
    }
    if (!"UNDER_REVIEW".equals(row.status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "status");
    }
    if (reviewer.equals(row.firstReviewer)) {
      throw new BusinessException(ErrorCode.CONFLICT, "reviewer");
    }
    String heirKyc = jdbc.query(
        "SELECT kyc_status FROM user_profile WHERE user_id = ?",
        rs -> rs.next() ? rs.getString(1) : null,
        row.heirUserId);
    if (!"APPROVED".equals(heirKyc)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "kyc");
    }
    String estateAccount = mainAccount(row.estateUserId);
    String heirAccount = mainAccount(row.heirUserId);
    jdbc.update("UPDATE user_profile SET account_status = 'FROZEN' WHERE user_id = ?", row.estateUserId);
    jdbc.update(
        "UPDATE fund_account SET status = 'FROZEN' WHERE account_no = ?",
        estateAccount);
    migrate(id, row.estateUserId, row.heirUserId, estateAccount, heirAccount);
    copyRoles(row.estateUserId, row.heirUserId);
    jdbc.update("UPDATE user_profile SET user_status = 'INHERITED' WHERE user_id = ?", row.estateUserId);
    jdbc.update(
        """
        UPDATE inheritance_case
        SET status = 'COMPLETED', second_reviewer = ?, updated_at = ?
        WHERE case_id = ?
        """,
        reviewer,
        Timestamp.from(now),
        id);
    return view(id, "COMPLETED", row.estateUserId, row.heirUserId);
  }

  /** 每个币种先给继承人开立 0 余额，再把原账户可用余额转过去。金额为 0 不记账。 */
  private void migrate(String caseId, String estateUserId, String heirUserId, String estateAccount, String heirAccount) {
    for (AccountBalance balance : ledger.balances(estateUserId)) {
      if (!estateAccount.equals(balance.accountId())) {
        continue;
      }
      ledger.openZero(heirAccount, balance.currency(), balance.scale(), heirUserId);
      if (balance.available() > 0) {
        ledger.transfer(
            reference(caseId, balance.currency()),
            estateAccount,
            heirAccount,
            balance.currency(),
            balance.scale(),
            balance.available());
      }
    }
  }

  /** 引用号要落在账本 128 字符以内。 */
  private static String reference(String caseId, String currency) {
    String reference = "inh:" + caseId + ":" + currency;
    if (reference.length() <= 128) {
      return reference;
    }
    return "inh:" + Digests.sha256(caseId + ":" + currency);
  }

  /** 原账户的角色复制给继承人，已有的不重复挂。 */
  private void copyRoles(String estateUserId, String heirUserId) {
    List<String> roles = jdbc.query(
        "SELECT role_code FROM rbac_user_role WHERE user_id = ?",
        (rs, row) -> rs.getString(1),
        estateUserId);
    for (String role : roles) {
      jdbc.update(
          """
          INSERT INTO rbac_user_role (user_id, role_code)
          SELECT ?, ? FROM DUAL
          WHERE NOT EXISTS (SELECT 1 FROM rbac_user_role WHERE user_id = ? AND role_code = ?)
          """,
          heirUserId,
          role,
          heirUserId,
          role);
    }
  }

  /** 主账户号。没有时拒绝迁移。 */
  private String mainAccount(String userId) {
    String account = jdbc.query(
        "SELECT account_no FROM user_fund_account WHERE user_id = ? AND fund_code = 'MAIN'",
        rs -> rs.next() ? rs.getString(1) : null,
        userId);
    if (account == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "account");
    }
    return account;
  }

  /** 用户必须已有档案。 */
  private void requireProfile(String userId, String name) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM user_profile WHERE user_id = ?", Integer.class, userId);
    if (count == null || count == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, name);
    }
  }

  /** 读出复核需要的列。 */
  private OpenCase load(String caseId) {
    return jdbc.query(
        "SELECT estate_user_id, heir_user_id, status, first_reviewer FROM inheritance_case WHERE case_id = ?",
        rs -> rs.next() ? new OpenCase(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null,
        caseId);
  }

  /** 材料最大 1MB。 */
  private static byte[] decode(String evidenceBase64) {
    try {
      byte[] body = Base64.getDecoder().decode(Ids.required(evidenceBase64, "evidence").trim());
      if (body.length == 0 || body.length > 1024 * 1024) {
        throw new IllegalArgumentException("evidence");
      }
      return body;
    } catch (IllegalArgumentException exception) {
      if ("evidence".equals(exception.getMessage())) {
        throw exception;
      }
      throw new IllegalArgumentException("evidence");
    }
  }

  /** 组装响应。 */
  private static CustomerViews.InheritanceView view(String caseId, String status, String estateUserId, String heirUserId) {
    return new CustomerViews.InheritanceView(caseId, status, estateUserId, heirUserId);
  }

  /** 复核时用到的案件字段。 */
  private record OpenCase(String estateUserId, String heirUserId, String status, String firstReviewer) {
  }
}
