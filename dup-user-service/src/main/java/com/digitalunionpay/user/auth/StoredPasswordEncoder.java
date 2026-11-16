package com.digitalunionpay.user.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 新密码写 Argon2id。库里已有的 PBKDF2 摘要仍能登录。 */
@Component
public final class StoredPasswordEncoder implements PasswordEncoder {

  /** 生成带随机盐的密码摘要。 */
  @Override
  public String encode(CharSequence rawPassword) {
    if (rawPassword == null) {
      throw new IllegalArgumentException("password");
    }
    return PasswordHashes.hash(rawPassword.toString());
  }

  /** 摘要格式不对或密码不一致时返回 false。 */
  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    if (rawPassword == null) {
      return false;
    }
    return PasswordHashes.matches(rawPassword.toString(), encodedPassword);
  }
}
