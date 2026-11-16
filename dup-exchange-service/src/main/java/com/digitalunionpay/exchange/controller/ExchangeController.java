package com.digitalunionpay.exchange.controller;

import com.digitalunionpay.exchange.ExchangePaths;
import com.digitalunionpay.exchange.LockRequest;
import com.digitalunionpay.exchange.PriceLock;
import com.digitalunionpay.exchange.service.ExchangeQuoteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class ExchangeController {

  private final ExchangeQuoteService exchange;

  /** 接入本进程的端口实现。 */
  public ExchangeController(ExchangeQuoteService exchange) {
    this.exchange = exchange;
  }

  /** 锁定报价。 */
  @PostMapping(ExchangePaths.LOCKS)
  public PriceLock lock(@RequestBody LockRequest request) {
    return exchange.lock(request.userId(), request.source(), request.targetCurrency());
  }
}
