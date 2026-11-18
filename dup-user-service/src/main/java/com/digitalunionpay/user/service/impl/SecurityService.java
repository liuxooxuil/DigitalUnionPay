package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.user.auth.Contacts;
import com.digitalunionpay.user.auth.PasswordPolicy;
import com.digitalunionpay.user.auth.VerificationCodes;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import com.digitalunionpay.user.service.CustomerForms;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 修改密码、绑定联系方式和二次验证开关。类不是 final，事务代理要生成子类。 */
@Service
public class SecurityService {

  private final RbacUserMapper users;

  private final VerificationCodes codes;

  private final PasswordEncoder passwords;

  private final JdbcTemplate jdbc;

  /** 接入账号、验证码和密码编码器。 */
  public SecurityService(RbacUserMapper users, VerificationCodes codes, PasswordEncoder passwords, JdbcTemplate jdbc) {
    if (passwords == null) {
      throw new IllegalArgumentException("passwords");
    }
    this.users = users;
    this.codes = codes;
    this.passwords = passwords;
    this.jdbc = jdbc;
  }

  /** 旧密码正确才换成新密码。新密码仍要满足字母加数字。 */
  @Transactional
  public void changePassword(String userId, CustomerForms.PasswordChange form) {
    RbacUserEntity user = required(userId);
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    if (!passwords.matches(Ids.required(form.oldPassword(), "password"), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "password");
    }
    String next = PasswordPolicy.require(form.newPassword());
    if (next.equals(form.oldPassword())) {
      throw new IllegalArgumentException("password");
    }
    jdbc.update("UPDATE rbac_user SET password_hash = ? WHERE user_id = ?", passwords.encode(next), userId);
  }

  /** 先消费 BIND 验证码，再写入手机号或邮箱。登录名保持注册时的那个。 */
  @Transactional
  public void bind(String userId, CustomerForms.BindContact form) {
    required(userId);
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String channel = VerificationCodes.channel(form.channel());
    String target = Contacts.target(channel, form.countryCode(), form.phone(), form.email());
    codes.consume(channel, target, "BIND", form.code());
    try {
      if ("SMS".equals(channel)) {
        jdbc.update("UPDATE rbac_user SET phone_e164 = ? WHERE user_id = ?", target, userId);
      } else {
        jdbc.update("UPDATE rbac_user SET email = ? WHERE user_id = ?", target, userId);
      }
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.CONFLICT, "SMS".equals(channel) ? "phone" : "email");
    }
  }

  /** 登录名对应的那一个联系方式不能解绑。另一个可以。 */
  @Transactional
  public void unbind(String userId, String channel) {
    Ids.required(userId, "userId");
    String kind = VerificationCodes.channel(channel);
    Contact contact = jdbc.query(
        "SELECT username, phone_e164, email FROM rbac_user WHERE user_id = ?",
        rs -> {
          if (!rs.next()) {
            return null;
          }
          return new Contact(rs.getString(1), rs.getString(2), rs.getString(3));
        },
        userId);
    if (contact == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "user");
    }
    if ("SMS".equals(kind)) {
      if (contact.phone == null || contact.username.equals(contact.phone)) {
        throw new BusinessException(ErrorCode.CONFLICT, "contact");
      }
      jdbc.update("UPDATE rbac_user SET phone_e164 = NULL WHERE user_id = ?", userId);
      return;
    }
    if (contact.email == null || contact.username.equals(contact.email)) {
      throw new BusinessException(ErrorCode.CONFLICT, "contact");
    }
    jdbc.update("UPDATE rbac_user SET email = NULL WHERE user_id = ?", userId);
  }

  /** 打开或关闭登录二次验证。 */
  @Transactional
  public void mfa(String userId, boolean enabled) {
    Ids.required(userId, "userId");
    jdbc.update("UPDATE rbac_user SET mfa_enabled = ? WHERE user_id = ?", enabled ? 1 : 0, userId);
  }

  /** 账号必须存在。 */
  private RbacUserEntity required(String userId) {
    RbacUserEntity user = users.findByUserId(Ids.required(userId, "userId"));
    if (user == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "user");
    }
    return user;
  }

  /** 登录名和两个联系方式。 */
  private record Contact(String username, String phone, String email) {
  }
}
