package com.digitalunionpay.matching.controller;

import com.digitalunionpay.matching.AcceptOrder;
import com.digitalunionpay.matching.MatchingPaths;
import com.digitalunionpay.matching.service.MatchOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class MatchingController {

  private final MatchOrderService orderBook;

  /** 接入本进程的端口实现。 */
  public MatchingController(MatchOrderService orderBook) {
    this.orderBook = orderBook;
  }

  /** 接受订单进入订单簿。 */
  @PostMapping(MatchingPaths.ORDERS)
  public void accept(@RequestBody AcceptOrder request) {
    orderBook.accept(request.orderId(), request.pair(), request.side(), request.price(), request.quantityMinor());
  }
}
