package com.digitalunionpay.user;

/** 用登录名和密码换访问 token。开启二次验证时要带上验证码。deviceId 为空时按 IP 记设备。 */
public record LoginCommand(String username, String password, String deviceId, String deviceName, String code) {
}
