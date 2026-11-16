package com.digitalunionpay.user.controller;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.support.AccessTokens;
import com.digitalunionpay.user.AccessCheck;
import com.digitalunionpay.user.AuthPort;
import com.digitalunionpay.user.IssuedToken;
import com.digitalunionpay.user.LoginCommand;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** 登录和给其他进程用的权限检查。这两条路径本身不要求已有 token。 */
@RestController
public class AuthController {

  private final AuthPort auth;

  /** 接入登录和权限检查。 */
  public AuthController(AuthPort auth) {
    this.auth = auth;
  }

  /** 签发访问 token。 */
  @PostMapping(AuthPaths.LOGIN)
  public IssuedToken login(@RequestBody LoginCommand command) {
    return auth.login(command);
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
