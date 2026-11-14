package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;

/** TCC 的 Try：按 reference 幂等预冻结。 */
public record FreezeRequest(String traceId, String reference, String accountId, Money amount) {

  public FreezeRequest {
    traceId = Ids.required(traceId, "traceId");
    reference = Ids.required(reference, "reference");
    accountId = Ids.required(accountId, "accountId");
    if (amount == null) {
      throw new IllegalArgumentException("amount");
    }
  }
}
