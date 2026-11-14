package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

public record Posting(String accountId, EntrySide side, Money amount) {

  public Posting {
    accountId = Ids.required(accountId, "accountId");
    if (side == null) {
      throw new IllegalArgumentException("side");
    }
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
