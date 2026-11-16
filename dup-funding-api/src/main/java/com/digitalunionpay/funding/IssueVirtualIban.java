package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;

/** 签发虚拟 IBAN 的请求。 */
public record IssueVirtualIban(String userId, String currency) {

  /** 拒绝空的用户和币种。 */
  public IssueVirtualIban {
    userId = Ids.required(userId, "userId");
    currency = Ids.required(currency, "currency");
  }
}
