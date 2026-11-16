package com.digitalunionpay.user.controller;

import com.digitalunionpay.user.OpenInheritanceCase;
import com.digitalunionpay.user.UserPaths;
import com.digitalunionpay.user.service.InheritanceCaseService;
import com.digitalunionpay.user.service.KycService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class UserController {

  private final KycService kycGate;

  private final InheritanceCaseService inheritance;

  /** 接入本进程的服务入口。 */
  public UserController(KycService kycGate, InheritanceCaseService inheritance) {
    this.kycGate = kycGate;
    this.inheritance = inheritance;
  }

  /** 查询该用户是否允许出入金。 */
  @GetMapping(UserPaths.FUNDING_ALLOWED)
  public boolean fundingAllowed(@PathVariable("userId") String userId) {
    return kycGate.fundingAllowed(userId);
  }

  /** 打开一笔继承案件。 */
  @PostMapping(UserPaths.INHERITANCE_CASES)
  public void open(@RequestBody OpenInheritanceCase request) {
    inheritance.open(request.caseId(), request.heirUserId());
  }
}
