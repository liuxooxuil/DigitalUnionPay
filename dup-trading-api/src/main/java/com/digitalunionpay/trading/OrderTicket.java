package com.digitalunionpay.trading;

import com.digitalunionpay.common.Ids;

/** 市价单回执中的订单号。 */
public record OrderTicket(String value) {

  /** 拒绝 null 和空白的订单号。 */
  public OrderTicket {
    value = Ids.required(value, "value");
  }
}
