package com.digitalunionpay.matching;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MatchingController {

  private final OrderBook orderBook;

  public MatchingController(OrderBook orderBook) {
    this.orderBook = orderBook;
  }

  @PostMapping(MatchingPaths.ORDERS)
  public void accept(@RequestBody AcceptOrder request) {
    orderBook.accept(request.orderId(), request.pair(), request.side(), request.price(), request.quantityMinor());
  }
}
