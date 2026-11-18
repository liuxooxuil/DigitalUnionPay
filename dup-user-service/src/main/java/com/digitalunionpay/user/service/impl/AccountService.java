package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.AccountBalance;
import com.digitalunionpay.ledger.StatementLine;
import com.digitalunionpay.support.FieldCipher;
import com.digitalunionpay.user.auth.LongIds;
import com.digitalunionpay.user.ledger.CustomerLedger;
import com.digitalunionpay.user.service.CustomerForms;
import com.digitalunionpay.user.service.CustomerViews;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 余额、流水、限额、同名收款账户和关闭申请。类不是 final，事务代理要生成子类。 */
@Service
public class AccountService {

  private final JdbcTemplate jdbc;

  private final CustomerLedger ledger;

  private final FieldCipher cipher;

  private final Clock clock;

  /** 接入档案、账本和字段加密。 */
  public AccountService(JdbcTemplate jdbc, CustomerLedger ledger, FieldCipher cipher, Clock clock) {
    this.jdbc = jdbc;
    this.ledger = ledger;
    this.cipher = cipher;
    this.clock = clock;
  }

  /** 档案上的用户状态、KYC 和账户状态。 */
  public CustomerViews.AccountSnapshot snapshot(String userId) {
    Ids.required(userId, "userId");
    CustomerViews.AccountSnapshot row = jdbc.query(
        "SELECT user_status, kyc_status, account_status, kyc_level FROM user_profile WHERE user_id = ?",
        rs -> rs.next()
            ? new CustomerViews.AccountSnapshot(userId, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4))
            : null,
        userId);
    if (row == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "user");
    }
    return row;
  }

  /** 每个资金子账户的各币种余额。账本还没有行时按资金定义返回 0。 */
  public List<CustomerViews.Holding> balances(String userId) {
    Ids.required(userId, "userId");
    List<AccountBalance> balances = ledger.balances(userId);
    List<FundAccount> accounts = accounts(userId);
    List<CustomerViews.Holding> holdings = new ArrayList<>();
    for (FundAccount account : accounts) {
      boolean found = false;
      for (AccountBalance balance : balances) {
        if (account.accountNo.equals(balance.accountId())) {
          found = true;
          holdings.add(new CustomerViews.Holding(
              account.accountNo,
              account.fundCode,
              account.fundKind,
              balance.currency(),
              balance.available(),
              balance.frozen(),
              balance.scale()));
        }
      }
      if (!found) {
        holdings.add(new CustomerViews.Holding(
            account.accountNo, account.fundCode, account.fundKind, account.currency, 0, 0, account.scale));
      }
    }
    return holdings;
  }

  /** 按时间和借贷方向筛选。type 与 side 相同。 */
  public List<StatementLine> statements(String userId, String from, String to, String side) {
    Ids.required(userId, "userId");
    parse(from, "from");
    parse(to, "to");
    String direction = side == null || side.isBlank() ? null : side.trim().toUpperCase(Locale.ROOT);
    if (direction != null && !"DEBIT".equals(direction) && !"CREDIT".equals(direction)) {
      throw new IllegalArgumentException("side");
    }
    return ledger.statements(userId, blank(from), blank(to), direction);
  }

  /** 单笔、日累计、月累计都按当前 KYC 等级。超出时拒绝。 */
  public CustomerViews.LimitView checkLimit(String userId, CustomerForms.LimitCheck form) {
    Ids.required(userId, "userId");
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    if (form.minorUnits() < 0 || form.scale() < 0 || form.scale() > 18) {
      throw new IllegalArgumentException("minorUnits");
    }
    String currency = Ids.required(form.currency(), "currency").trim().toUpperCase(Locale.ROOT);
    String level = jdbc.query(
        "SELECT kyc_level FROM user_profile WHERE user_id = ?",
        rs -> rs.next() ? rs.getString(1) : null,
        userId);
    if (level == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "user");
    }
    Limit limit = jdbc.query(
        "SELECT single_minor, daily_minor, monthly_minor, currency FROM kyc_limit WHERE kyc_level = ?",
        rs -> rs.next() ? new Limit(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4)) : null,
        level);
    if (limit == null || !limit.currency.equals(currency)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "currency");
    }
    Integer scale = jdbc.queryForObject(
        "SELECT scale FROM fund WHERE currency = ? AND status = 'OPEN' ORDER BY id LIMIT 1",
        Integer.class,
        currency);
    if (scale == null || scale != form.scale()) {
      throw new IllegalArgumentException("scale");
    }
    LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    long usedDaily = used(userId, currency, today.atStartOfDay().toInstant(ZoneOffset.UTC));
    long usedMonthly = used(userId, currency, today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    if (form.minorUnits() > limit.single || usedDaily + form.minorUnits() > limit.daily || usedMonthly + form.minorUnits() > limit.monthly) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "limit");
    }
    return new CustomerViews.LimitView(limit.single, limit.daily, limit.monthly, usedDaily, usedMonthly);
  }

  /** 户名必须和已通过的 KYC 姓名一致。账号加密保存。 */
  @Transactional
  public CustomerViews.BankAccountView bindBank(String userId, CustomerForms.BindBank form) {
    Ids.required(userId, "userId");
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String holder = Ids.required(form.holderName(), "holderName").trim();
    String number = Ids.required(form.accountNumber(), "accountNumber").trim();
    String bank = Ids.required(form.bankName(), "bankName").trim();
    if (holder.length() > 128 || bank.length() > 128 || number.length() > 64) {
      throw new IllegalArgumentException("accountNumber");
    }
    String approved = jdbc.query(
        """
        SELECT full_name FROM kyc_case
        WHERE user_id = ? AND status = 'APPROVED'
        ORDER BY decided_at DESC, id DESC LIMIT 1
        """,
        rs -> rs.next() ? rs.getString(1) : null,
        userId);
    if (approved == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "kyc");
    }
    if (!approved.trim().equals(holder)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "name");
    }
    long id = LongIds.next();
    jdbc.update(
        "INSERT INTO bank_account (id, user_id, holder_name, account_cipher, bank_name, status, created_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)",
        id,
        userId,
        holder,
        cipher.encrypt(number),
        bank,
        Timestamp.from(clock.instant()));
    return new CustomerViews.BankAccountView(id, holder, bank, last4(number));
  }

  /** 可用或冻结余额不为 0 时不能申请关闭。 */
  @Transactional
  public CustomerViews.ClosureView requestClosure(String userId) {
    Ids.required(userId, "userId");
    assertZero(userId);
    Integer pending = jdbc.queryForObject(
        "SELECT COUNT(*) FROM closure_request WHERE user_id = ? AND status = 'PENDING'",
        Integer.class,
        userId);
    if (pending != null && pending > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "closure");
    }
    long id = LongIds.next();
    jdbc.update(
        "INSERT INTO closure_request (id, user_id, status, requested_at, decided_at) VALUES (?, ?, 'PENDING', ?, NULL)",
        id,
        userId,
        Timestamp.from(clock.instant()));
    return new CustomerViews.ClosureView(id, "PENDING");
  }

  /** 通过后账户关闭并撤销全部会话。余额若在审核期间又变多，仍然拒绝。 */
  @Transactional
  public CustomerViews.ClosureView decideClosure(String userId, boolean approve) {
    Ids.required(userId, "userId");
    Long id = jdbc.query(
        "SELECT id FROM closure_request WHERE user_id = ? AND status = 'PENDING' ORDER BY requested_at DESC LIMIT 1",
        rs -> rs.next() ? rs.getLong(1) : null,
        userId);
    if (id == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "closure");
    }
    Instant now = clock.instant();
    if (!approve) {
      jdbc.update(
          "UPDATE closure_request SET status = 'REJECTED', decided_at = ? WHERE id = ?",
          Timestamp.from(now),
          id);
      return new CustomerViews.ClosureView(id, "REJECTED");
    }
    assertZero(userId);
    jdbc.update(
        "UPDATE closure_request SET status = 'APPROVED', decided_at = ? WHERE id = ?",
        Timestamp.from(now),
        id);
    jdbc.update("UPDATE user_profile SET account_status = 'CLOSED' WHERE user_id = ?", userId);
    jdbc.update(
        "UPDATE fund_account SET status = 'CLOSED' WHERE account_no IN (SELECT account_no FROM user_fund_account WHERE user_id = ?)",
        userId);
    jdbc.update(
        "UPDATE auth_token SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
        Timestamp.from(now),
        userId);
    return new CustomerViews.ClosureView(id, "APPROVED");
  }

  /** 任意币种还有可用或冻结余额都不能关户。 */
  private void assertZero(String userId) {
    for (AccountBalance balance : ledger.balances(userId)) {
      if (balance.available() > 0 || balance.frozen() > 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "balance");
      }
    }
  }

  /** 从窗口起点累加借方流水。 */
  private long used(String userId, String currency, Instant from) {
    long total = 0;
    for (StatementLine line : ledger.statements(userId, from.toString(), null, "DEBIT")) {
      if (currency.equals(line.currency())) {
        total += line.minorUnits();
      }
    }
    return total;
  }

  /** 这个用户名下的资金子账户。 */
  private List<FundAccount> accounts(String userId) {
    return jdbc.query(
        """
        SELECT a.account_no, a.fund_code, f.fund_kind, f.currency, f.scale
        FROM user_fund_account u
        JOIN fund_account a ON a.account_no = u.account_no
        JOIN fund f ON f.fund_code = a.fund_code
        WHERE u.user_id = ?
        ORDER BY a.account_no
        """,
        (rs, row) -> new FundAccount(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)),
        userId);
  }

  /** 空白时间不当成筛选条件。格式不对时拒绝。 */
  private static void parse(String value, String name) {
    if (value == null || value.isBlank()) {
      return;
    }
    try {
      Instant.parse(value.trim());
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException(name);
    }
  }

  /** 空白查询参数不往下传。 */
  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** 账号后四位。更短时原样返回。 */
  private static String last4(String number) {
    return number.length() <= 4 ? number : number.substring(number.length() - 4);
  }

  /** 一个资金子账户。 */
  private record FundAccount(String accountNo, String fundCode, String fundKind, String currency, int scale) {
  }

  /** 某一 KYC 等级的限额。金额是最小货币单位。 */
  private record Limit(long single, long daily, long monthly, String currency) {
  }
}
