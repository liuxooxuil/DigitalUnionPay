package com.digitalunionpay.trading;

import com.digitalunionpay.common.Ids;

public record OrderTicket(String value) {

  public OrderTicket {
    value = Ids.required(value, "value");
  }
}
