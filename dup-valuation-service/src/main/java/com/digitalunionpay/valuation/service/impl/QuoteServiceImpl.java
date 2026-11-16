package com.digitalunionpay.valuation.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.valuation.mapper.QuoteMapper;
import com.digitalunionpay.valuation.service.QuoteService;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class QuoteServiceImpl implements QuoteService {

  private final QuoteMapper quoteMapper;

  /** 接入本聚合的 Mapper。 */
  public QuoteServiceImpl(QuoteMapper quoteMapper) {
    this.quoteMapper = quoteMapper;
  }

  /** 校验源金额和目标币种后拒绝报价。 */
  @Override
  public Money convert(Money source, String targetCurrency) {
    if (source == null) {
      throw new IllegalArgumentException("source");
    }
    Ids.required(targetCurrency, "targetCurrency");
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "ValuationPort.convert");
  }
}
