package com.digitalunionpay.valuation;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValuationController {

  private final ValuationPort valuation;

  public ValuationController(ValuationPort valuation) {
    this.valuation = valuation;
  }

  @PostMapping(ValuationPaths.CONVERSIONS)
  public Money convert(@RequestBody ConversionRequest request) {
    return valuation.convert(request.source(), request.targetCurrency());
  }
}
