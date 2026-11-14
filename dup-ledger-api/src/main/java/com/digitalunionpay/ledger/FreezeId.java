package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;

public record FreezeId(String value) {

  public FreezeId {
    value = Ids.required(value, "freezeId");
  }
}
