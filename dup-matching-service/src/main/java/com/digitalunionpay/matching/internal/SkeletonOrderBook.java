package com.digitalunionpay.matching.internal;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.matching.OrderBook;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.valuation.ValuationPort;

public final class SkeletonOrderBook implements OrderBook {

  private final ValuationPort valuation;
  private final TccLedger ledger;

  public SkeletonOrderBook(ValuationPort valuation, TccLedger ledger) {
    if (valuation == null || ledger == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.valuation = valuation;
    this.ledger = ledger;
  }

  @Override
  public void accept(String orderId, String pair, String side, Money price, long quantityMinor) {
    GlobalTransactions.run("matching.accept", () -> acceptOrder(orderId, pair, side, price, quantityMinor));
  }

  private void acceptOrder(String orderId, String pair, String side, Money price, long quantityMinor) {
    Ids.required(orderId, "orderId");
    Ids.required(pair, "pair");
    Ids.required(side, "side");
    if (price == null || quantityMinor <= 0) {
      throw new IllegalArgumentException("price and quantity");
    }
    if (valuation == null || ledger == null) {
      throw new IllegalStateException("collaborators");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "OrderBook.accept");
  }
}
