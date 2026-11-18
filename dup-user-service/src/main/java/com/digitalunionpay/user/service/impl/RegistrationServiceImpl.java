package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.AccountStatus;
import com.digitalunionpay.user.KycStatus;
import com.digitalunionpay.user.RegisterCommand;
import com.digitalunionpay.user.RegisteredUser;
import com.digitalunionpay.user.RegistrationPort;
import com.digitalunionpay.user.UserStatus;
import com.digitalunionpay.user.auth.Contacts;
import com.digitalunionpay.user.auth.LongIds;
import com.digitalunionpay.user.auth.PasswordPolicy;
import com.digitalunionpay.user.auth.VerificationCodes;
import com.digitalunionpay.user.entity.FundAccountEntity;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.entity.UserFundAccountEntity;
import com.digitalunionpay.user.entity.UserProfileEntity;
import com.digitalunionpay.user.ledger.CustomerLedger;
import com.digitalunionpay.user.mapper.FundAccountMapper;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import com.digitalunionpay.user.mapper.UserFundAccountMapper;
import com.digitalunionpay.user.mapper.UserProfileMapper;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 手机号或邮箱注册。成功后创建待 KYC 的主账户，并在账本开立余额为 0 的账户。类不是 final，事务代理要生成子类。 */
@Service
public class RegistrationServiceImpl implements RegistrationPort {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final RbacUserMapper users;

  private final UserProfileMapper profiles;

  private final FundAccountMapper accounts;

  private final UserFundAccountMapper links;

  private final VerificationCodes codes;

  private final CustomerLedger ledger;

  private final PasswordEncoder passwords;

  private final JdbcTemplate jdbc;

  private final Clock clock;

  private final int ipLimit;

  /** 接入档案、验证码、账本和同一 IP 的每小时上限。 */
  public RegistrationServiceImpl(
      RbacUserMapper users,
      UserProfileMapper profiles,
      FundAccountMapper accounts,
      UserFundAccountMapper links,
      VerificationCodes codes,
      CustomerLedger ledger,
      PasswordEncoder passwords,
      JdbcTemplate jdbc,
      Clock clock,
      @Value("${dup.auth.register-ip-limit:5}") int ipLimit) {
    if (passwords == null) {
      throw new IllegalArgumentException("passwords");
    }
    this.users = users;
    this.profiles = profiles;
    this.accounts = accounts;
    this.links = links;
    this.codes = codes;
    this.ledger = ledger;
    this.passwords = passwords;
    this.jdbc = jdbc;
    this.clock = clock;
    this.ipLimit = ipLimit;
  }

  /** 验证码、IP 频率和唯一性都通过后才落库。失败整笔回滚。 */
  @Override
  @Transactional
  public RegisteredUser register(RegisterCommand command, String clientIp) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    String ip = Ids.required(clientIp, "ip");
    String channel = VerificationCodes.channel(command.channel());
    String username = Contacts.target(channel, command.countryCode(), command.phone(), command.email());
    String password = PasswordPolicy.require(command.password());
    codes.consume(channel, username, "REGISTER", command.code());
    Integer recent = jdbc.queryForObject(
        "SELECT COUNT(*) FROM registration_event WHERE client_ip = ? AND created_at >= ?",
        Integer.class,
        ip,
        Timestamp.from(clock.instant().minus(1, ChronoUnit.HOURS)));
    if (recent != null && recent >= ipLimit) {
      throw new BusinessException(ErrorCode.RATE_LIMITED, "ip");
    }
    if (users.findByUsername(username) != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "SMS".equals(channel) ? "phone" : "email");
    }
    String userId = newUserId();
    RbacUserEntity user = new RbacUserEntity();
    user.setUserId(userId);
    user.setUsername(username);
    user.setPasswordHash(passwords.encode(password));
    user.setStatus("ACTIVE");
    if ("SMS".equals(channel)) {
      user.setPhoneE164(username);
    } else {
      user.setEmail(username);
    }
    UserProfileEntity profile = new UserProfileEntity();
    profile.setUserId(userId);
    profile.setUserStatus(UserStatus.PENDING_KYC.name());
    profile.setKycStatus(KycStatus.NOT_SUBMITTED.name());
    profile.setAccountStatus(AccountStatus.NORMAL.name());
    profile.setKycLevel("NONE");
    profile.setCreatedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    String accountNo = newAccountNo();
    FundAccountEntity account = new FundAccountEntity();
    account.setAccountNo(accountNo);
    account.setFundCode("MAIN");
    account.setStatus(UserStatus.PENDING_KYC.name());
    account.setLedgerAccountId(accountNo);
    account.setCreatedAt(profile.getCreatedAt());
    UserFundAccountEntity link = new UserFundAccountEntity();
    link.setUserId(userId);
    link.setFundCode("MAIN");
    link.setAccountNo(accountNo);
    try {
      users.insert(user);
      profiles.insert(profile);
      accounts.insert(account);
      links.insert(link);
      jdbc.update(
          "INSERT INTO registration_event (id, client_ip, user_id, created_at) VALUES (?, ?, ?, ?)",
          LongIds.next(),
          ip,
          userId,
          Timestamp.from(clock.instant()));
      grantCustomer(userId);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.CONFLICT, "SMS".equals(channel) ? "phone" : "email");
    }
    ledger.openZero(accountNo, "CNY", 2, userId);
    return new RegisteredUser(userId, username, profile.getUserStatus(), profile.getKycStatus(), accountNo);
  }

  /** 客户角色和三条接口权限不存在时补上，再挂到这个用户。测试会清空权限表，所以这里也要能补。 */
  private void grantCustomer(String userId) {
    jdbc.update(
        "INSERT INTO rbac_role (id, code, name) SELECT 100, 'CUSTOMER', '客户' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_role WHERE code = 'CUSTOMER')");
    jdbc.update(
        "INSERT INTO rbac_permission (id, code, http_method, path_pattern) SELECT 100, 'CUSTOMER_USERS', '*', '/internal/users/**' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'CUSTOMER_USERS')");
    jdbc.update(
        "INSERT INTO rbac_permission (id, code, http_method, path_pattern) SELECT 101, 'CUSTOMER_SESSIONS', '*', '/internal/sessions/**' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'CUSTOMER_SESSIONS')");
    jdbc.update(
        "INSERT INTO rbac_permission (id, code, http_method, path_pattern) SELECT 102, 'CUSTOMER_SECURITY', '*', '/internal/auth/security/**' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'CUSTOMER_SECURITY')");
    jdbc.update(
        "INSERT INTO rbac_role_permission (role_code, permission_code) SELECT 'CUSTOMER', 'CUSTOMER_USERS' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'CUSTOMER' AND permission_code = 'CUSTOMER_USERS')");
    jdbc.update(
        "INSERT INTO rbac_role_permission (role_code, permission_code) SELECT 'CUSTOMER', 'CUSTOMER_SESSIONS' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'CUSTOMER' AND permission_code = 'CUSTOMER_SESSIONS')");
    jdbc.update(
        "INSERT INTO rbac_role_permission (role_code, permission_code) SELECT 'CUSTOMER', 'CUSTOMER_SECURITY' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'CUSTOMER' AND permission_code = 'CUSTOMER_SECURITY')");
    jdbc.update(
        "INSERT INTO rbac_user_role (user_id, role_code) SELECT ?, 'CUSTOMER' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rbac_user_role WHERE user_id = ? AND role_code = 'CUSTOMER')",
        userId,
        userId);
  }

  /** 业务用户号。 */
  private static String newUserId() {
    byte[] raw = new byte[16];
    RANDOM.nextBytes(raw);
    return "u" + HexFormat.of().formatHex(raw);
  }

  /** 主账户号，同时作为账本 account_id。 */
  private static String newAccountNo() {
    byte[] raw = new byte[8];
    RANDOM.nextBytes(raw);
    return "MA" + HexFormat.of().formatHex(raw);
  }
}
