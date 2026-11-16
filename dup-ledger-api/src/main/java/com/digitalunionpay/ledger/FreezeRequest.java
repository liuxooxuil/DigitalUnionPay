package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** TCC 的 Try：按 reference 幂等预冻结。 */
public record FreezeRequest(String traceId, String reference, String accountId, Money amount) {

  /** 拒绝空白的链路号、幂等参考、账户，以及空金额。 */
  public FreezeRequest {
    traceId = Ids.required(traceId, "traceId");
    reference = Ids.required(reference, "reference");
    accountId = Ids.required(accountId, "accountId");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
