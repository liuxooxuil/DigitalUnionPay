package com.digitalunionpay.user;

/** 用用户名和密码换访问 token。 */
public record LoginCommand(String username, String password) {
}
