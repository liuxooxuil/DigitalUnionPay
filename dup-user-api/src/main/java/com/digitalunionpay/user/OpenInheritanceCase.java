package com.digitalunionpay.user;

import com.digitalunionpay.common.Ids;

/** 打开继承案件的请求。 */
public record OpenInheritanceCase(String caseId, String heirUserId) {

  /** 拒绝空白的案件号和继承人。 */
  public OpenInheritanceCase {
    caseId = Ids.required(caseId, "caseId");
    heirUserId = Ids.required(heirUserId, "heirUserId");
  }
}
