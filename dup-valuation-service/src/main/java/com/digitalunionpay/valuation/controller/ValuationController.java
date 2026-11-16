package com.digitalunionpay.valuation.controller;

import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.valuation.ConversionRequest;
import com.digitalunionpay.valuation.ValuationPaths;
import com.digitalunionpay.valuation.service.QuoteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class ValuationController {

  private final QuoteService valuation;

  /** 接入本进程的服务入口。 */
  public ValuationController(QuoteService valuation) {
    this.valuation = valuation;
  }

  /** 受理一次换汇报价。 */
  @PostMapping(ValuationPaths.CONVERSIONS)
  public Money convert(@RequestBody ConversionRequest request) {
    return valuation.convert(request.source(), request.targetCurrency());
  }
}
