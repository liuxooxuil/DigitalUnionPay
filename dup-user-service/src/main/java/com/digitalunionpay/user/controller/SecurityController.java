package com.digitalunionpay.user.controller;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.user.auth.CurrentUser;
import com.digitalunionpay.user.service.CustomerForms;
import com.digitalunionpay.user.service.impl.SecurityService;
import com.digitalunionpay.user.service.impl.SessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 密码、联系方式和二次验证。这些路径要求已经登录。 */
@RestController
public class SecurityController {

  private final CurrentUser current;

  private final SecurityService security;

  private final SessionService sessions;

  /** 接入当前用户、安全设置和生物识别绑定。 */
  public SecurityController(CurrentUser current, SecurityService security, SessionService sessions) {
    this.current = current;
    this.security = security;
    this.sessions = sessions;
  }

  /** 修改登录密码。 */
  @PostMapping(AuthPaths.SECURITY_PASSWORD)
  public void password(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.PasswordChange form) {
    security.changePassword(current.require(authorization).userId(), form);
  }

  /** 绑定手机号或邮箱。要先拿到 BIND 验证码。 */
  @PostMapping(AuthPaths.SECURITY_CONTACTS)
  public void bind(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.BindContact form) {
    security.bind(current.require(authorization).userId(), form);
  }

  /** 解绑不是登录名的那个联系方式。 */
  @DeleteMapping(AuthPaths.SECURITY_CONTACTS)
  public void unbind(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestParam("channel") String channel) {
    security.unbind(current.require(authorization).userId(), channel);
  }

  /** 打开或关闭二次验证。 */
  @PostMapping(AuthPaths.SECURITY_MFA)
  public void mfa(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.MfaSwitch form) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    security.mfa(current.require(authorization).userId(), form.enabled());
  }

  /** 绑定当前用户这台设备的生物识别公钥。 */
  @PostMapping(AuthPaths.SECURITY_DEVICES)
  public void device(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.BindBiometric form) {
    sessions.bind(current.require(authorization).userId(), form);
  }
}
