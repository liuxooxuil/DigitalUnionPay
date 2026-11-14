package com.digitalunionpay.user;

import com.digitalunionpay.common.Ids;

public record OpenInheritanceCase(String caseId, String heirUserId) {

  public OpenInheritanceCase {
    caseId = Ids.required(caseId, "caseId");
    heirUserId = Ids.required(heirUserId, "heirUserId");
  }
}
