package com.digitalunionpay.user;

import com.digitalunionpay.common.Ids;

/** 打开继承案件的请求。材料原文只在这一次请求里出现，库里只留哈希。 */
public record OpenInheritanceCase(
    String caseId,
    String estateUserId,
    String heirUserId,
    String applicantRole,
    String evidenceBase64) {

  /** 拒绝空白的案件号、双方用户和材料。 */
  public OpenInheritanceCase {
    caseId = Ids.required(caseId, "caseId");
    estateUserId = Ids.required(estateUserId, "estateUserId");
    heirUserId = Ids.required(heirUserId, "heirUserId");
    applicantRole = Ids.required(applicantRole, "applicantRole");
    evidenceBase64 = Ids.required(evidenceBase64, "evidence");
  }
}
