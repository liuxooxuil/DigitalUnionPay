package com.digitalunionpay.user;

/** 用短信或邮箱验证码注册。channel 是 SMS 或 EMAIL。 */
public record RegisterCommand(
    String channel,
    String countryCode,
    String phone,
    String email,
    String code,
    String password) {
}
