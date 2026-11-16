package com.digitalunionpay.matching.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.matching.mapper.MatchOrderMapper;
import com.digitalunionpay.matching.service.MatchOrderService;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.valuation.ValuationPort;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class MatchOrderServiceImpl implements MatchOrderService {

  private final ValuationPort valuation;
  private final TccLedger ledger;
  private final MatchOrderMapper matchOrderMapper;

  /** 接入协作者和本聚合的 Mapper。 */
  public MatchOrderServiceImpl(ValuationPort valuation, TccLedger ledger, MatchOrderMapper matchOrderMapper) {
    if (valuation == null || ledger == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.valuation = valuation;
    this.ledger = ledger;
    this.matchOrderMapper = matchOrderMapper;
  }

  /** 开启 Seata 全局事务后执行接单辅助方法。 */
  @Override
  public void accept(String orderId, String pair, String side, Money price, long quantityMinor) {
    GlobalTransactions.run("matching.accept", () -> acceptOrder(orderId, pair, side, price, quantityMinor));
  }

  /** 校验入参和协作者，业务尚未实现。 */
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
