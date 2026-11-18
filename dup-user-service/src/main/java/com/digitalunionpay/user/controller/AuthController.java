package com.digitalunionpay.user.controller;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.support.AccessTokens;
import com.digitalunionpay.user.AccessCheck;
import com.digitalunionpay.user.AuthPort;
import com.digitalunionpay.user.IssuedToken;
import com.digitalunionpay.user.LoginCommand;
import com.digitalunionpay.user.RegisterCommand;
import com.digitalunionpay.user.RegisteredUser;
import com.digitalunionpay.user.RegistrationPort;
import com.digitalunionpay.user.auth.ClientIps;
import com.digitalunionpay.user.auth.CurrentUser;
import com.digitalunionpay.user.service.CustomerForms;
import com.digitalunionpay.user.service.CustomerViews;
import com.digitalunionpay.user.service.impl.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** 注册、登录和给其他进程用的权限检查。这几条路径本身不要求已有 token。 */
@RestController
public class AuthController {

  private final AuthPort auth;

  private final RegistrationPort registration;

  private final SessionService sessions;

  private final CurrentUser current;

  /** 接入注册、登录、验证码和权限检查。 */
  public AuthController(AuthPort auth, RegistrationPort registration, SessionService sessions, CurrentUser current) {
    this.auth = auth;
    this.registration = registration;
    this.sessions = sessions;
    this.current = current;
  }

  /** 发送短信或邮箱验证码。响应里没有明文。绑定用途要带登录 token。 */
  @PostMapping(AuthPaths.VERIFICATION_CODES)
  public void send(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody CustomerForms.SendCode form) {
    String userId = null;
    if ("BIND".equalsIgnoreCase(form == null || form.purpose() == null ? "" : form.purpose())) {
      userId = current.require(authorization).userId();
    }
    sessions.send(form, userId);
  }

  /** 创建登录账号、待 KYC 档案和余额为 0 的主账户。不签发 token。 */
  @PostMapping(AuthPaths.REGISTER)
  public RegisteredUser register(@RequestBody RegisterCommand command, HttpServletRequest request) {
    return registration.register(command, ClientIps.of(request));
  }

  /** 用密码签发访问 token。连续失败 5 次锁定 30 分钟。 */
  @PostMapping(AuthPaths.LOGIN)
  public IssuedToken login(@RequestBody LoginCommand command, HttpServletRequest request) {
    return auth.login(command, ClientIps.of(request));
  }

  /** 用短信或邮箱验证码签发访问 token。 */
  @PostMapping(AuthPaths.LOGIN_CODE)
  public IssuedToken loginWithCode(@RequestBody CustomerForms.CodeLogin form, HttpServletRequest request) {
    return sessions.loginWithCode(form, ClientIps.of(request));
  }

  /** 签发一次性生物识别挑战。 */
  @PostMapping(AuthPaths.BIOMETRIC_CHALLENGES)
  public CustomerViews.Challenge challenge(@RequestBody CustomerForms.BiometricChallenge form) {
    return new CustomerViews.Challenge(sessions.challenge(form));
  }

  /** 用已绑定设备的签名签发访问 token。 */
  @PostMapping(AuthPaths.LOGIN_BIOMETRIC)
  public IssuedToken loginWithBiometric(@RequestBody CustomerForms.BiometricLogin form, HttpServletRequest request) {
    return sessions.loginWithBiometric(form, ClientIps.of(request));
  }

  /** 按请求头里的 token 检查目标接口。没有权限时由统一异常处理返回。 */
  @PostMapping(AuthPaths.ACCESS)
  public void access(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody AccessCheck check) {
    if (check == null) {
      throw new IllegalArgumentException("check");
    }
    auth.check(AccessTokens.bearer(authorization), check.method(), check.path());
  }
}
