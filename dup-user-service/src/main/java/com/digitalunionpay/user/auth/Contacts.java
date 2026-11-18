package com.digitalunionpay.user.auth;

import com.digitalunionpay.common.Ids;
import java.util.Locale;

/** 把手机号和邮箱收成登录名。手机号保存为 E.164。 */
public final class Contacts {

  /** 禁止实例化。 */
  private Contacts() {
  }

  /** 国际区号加国内号码。只保留数字，不接受空白和符号。 */
  public static String phone(String countryCode, String national) {
    String country = digits(Ids.required(countryCode, "countryCode"), 1, 4, "countryCode");
    String number = digits(Ids.required(national, "phone"), 4, 15, "phone");
    return "+" + country + number;
  }

  /** 按通道收成登录名。短信要区号和号码，邮箱忽略区号。 */
  public static String target(String channel, String countryCode, String phone, String email) {
    if ("SMS".equals(channel)) {
      return phone(countryCode, phone);
    }
    return Contacts.email(email);
  }

  /** 邮箱转成小写。格式不对时拒绝。 */
  public static String email(String value) {
    String email = Ids.required(value, "email").trim().toLowerCase(Locale.ROOT);
    if (email.length() > 254 || !email.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}")) {
      throw new IllegalArgumentException("email");
    }
    return email;
  }

  /** 目标必须是纯数字，长度在给定范围内。 */
  private static String digits(String value, int min, int max, String name) {
    String digits = value.trim();
    if (digits.length() < min || digits.length() > max || !digits.chars().allMatch(Character::isDigit)) {
      throw new IllegalArgumentException(name);
    }
    return digits;
  }
}
