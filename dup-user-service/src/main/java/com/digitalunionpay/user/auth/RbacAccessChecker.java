package com.digitalunionpay.user.auth;

import com.digitalunionpay.support.AccessChecker;
import com.digitalunionpay.user.AuthPort;
import org.springframework.stereotype.Component;

/** 用户服务本地查 RBAC，不再远程调用自己。 */
@Component
public final class RbacAccessChecker implements AccessChecker {

  private final AuthPort auth;

  /** 接入本进程的权限检查。 */
  public RbacAccessChecker(AuthPort auth) {
    this.auth = auth;
  }

  /** 交给登录服务按 token 和权限表判断。 */
  @Override
  public void check(String token, String method, String path) {
    auth.check(token, method, path);
  }
}
