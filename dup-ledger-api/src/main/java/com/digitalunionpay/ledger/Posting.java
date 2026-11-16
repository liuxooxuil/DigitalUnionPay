package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** 分录中的一条：账户、方向和金额。 */
public record Posting(String accountId, EntrySide side, Money amount) {

  /** 拒绝空白账户、空方向和空金额。 */
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
