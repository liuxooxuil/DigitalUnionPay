package com.digitalunionpay.user.auth;

import com.digitalunionpay.common.Ids;

/** 密码 8 到 20 位，必须同时包含字母和数字，特殊字符可选。 */
public final class PasswordPolicy {

  private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";

  /** 禁止实例化。 */
  private PasswordPolicy() {
  }

  /** 不合规时拒绝。通过时原样返回，不截断。 */
  public static String require(String value) {
    String password = Ids.required(value, "password");
    if (password.length() < 8 || password.length() > 20) {
      throw new IllegalArgumentException("password");
    }
    boolean letter = false;
    boolean digit = false;
    for (int i = 0; i < password.length(); i++) {
      char ch = password.charAt(i);
      if (Character.isLetter(ch)) {
        letter = true;
      } else if (Character.isDigit(ch)) {
        digit = true;
      } else if (SPECIAL.indexOf(ch) < 0) {
        throw new IllegalArgumentException("password");
      }
    }
    if (!letter || !digit) {
      throw new IllegalArgumentException("password");
    }
    return password;
  }
}
