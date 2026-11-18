package com.digitalunionpay.user;

/** 用手机号或邮箱注册，并开立余额为 0 的主账户。 */
public interface RegistrationPort {

  /** 验证码正确且手机号或邮箱未被占用时创建用户和主账户。clientIp 计入每小时注册次数。 */
  RegisteredUser register(RegisterCommand command, String clientIp);
}
