package com.digitalunionpay.exchange;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExchangeController {

  private final ExchangePort exchange;

  public ExchangeController(ExchangePort exchange) {
    this.exchange = exchange;
  }

  @PostMapping(ExchangePaths.LOCKS)
  public PriceLock lock(@RequestBody LockRequest request) {
    return exchange.lock(request.userId(), request.source(), request.targetCurrency());
  }
}
