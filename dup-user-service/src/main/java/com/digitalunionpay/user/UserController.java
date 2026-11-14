package com.digitalunionpay.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  private final KycGate kycGate;
  private final InheritanceCasePort inheritance;

  public UserController(KycGate kycGate, InheritanceCasePort inheritance) {
    this.kycGate = kycGate;
    this.inheritance = inheritance;
  }

  @GetMapping(UserPaths.FUNDING_ALLOWED)
  public boolean fundingAllowed(@PathVariable("userId") String userId) {
    return kycGate.fundingAllowed(userId);
  }

  @PostMapping(UserPaths.INHERITANCE_CASES)
  public void open(@RequestBody OpenInheritanceCase request) {
    inheritance.open(request.caseId(), request.heirUserId());
  }
}
