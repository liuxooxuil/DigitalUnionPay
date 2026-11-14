package com.digitalunionpay.funding;

import com.digitalunionpay.common.Ids;

public record IssueVirtualIban(String userId, String currency) {

  public IssueVirtualIban {
    userId = Ids.required(userId, "userId");
    currency = Ids.required(currency, "currency");
  }
}
