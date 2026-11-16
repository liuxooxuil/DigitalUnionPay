package com.digitalunionpay.user.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 把现有 PBKDF2 摘要接到 Spring Security。库里的 iterations$salt$hash 格式不变。 */
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
