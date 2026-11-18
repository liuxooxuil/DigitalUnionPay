package com.digitalunionpay.user.controller;

import com.digitalunionpay.user.UserPaths;
import com.digitalunionpay.user.auth.CurrentUser;
import com.digitalunionpay.user.service.CustomerViews;
import com.digitalunionpay.user.service.impl.SessionService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录设备。空闲超过配置时间的会话在这里会被拒绝。 */
@RestController
public class SessionController {

  private final CurrentUser current;

  private final SessionService sessions;

  /** 接入当前用户和会话表。 */
  public SessionController(CurrentUser current, SessionService sessions) {
    this.current = current;
    this.sessions = sessions;
  }

  /** 列出仍在线的设备。 */
  @GetMapping(UserPaths.SESSIONS)
  public List<CustomerViews.SessionDevice> devices(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    CurrentUser.Actor actor = current.require(authorization);
    return sessions.devices(actor.userId(), actor.sessionId());
  }

  /** 远程强制下线一台自己的设备。 */
  @DeleteMapping(UserPaths.SESSION)
  public void revoke(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @PathVariable("sessionId") long sessionId) {
    sessions.revoke(current.require(authorization).userId(), sessionId);
  }
}
