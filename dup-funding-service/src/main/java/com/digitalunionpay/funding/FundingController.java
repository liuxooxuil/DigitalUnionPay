package com.digitalunionpay.funding;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FundingController {

  private final FiatCollectionPort fiat;
  private final WithdrawalPort withdrawals;
  private final FundingCoordinator coordinator;

  public FundingController(FiatCollectionPort fiat, WithdrawalPort withdrawals, FundingCoordinator coordinator) {
    this.fiat = fiat;
    this.withdrawals = withdrawals;
    this.coordinator = coordinator;
  }

  @PostMapping(FundingPaths.VIRTUAL_IBANS)
  public IssuedIban issueVirtualIban(@RequestBody IssueVirtualIban request) {
    return new IssuedIban(fiat.issueVirtualIban(request.userId(), request.currency()));
  }

  @PostMapping(FundingPaths.WITHDRAWALS)
  public WithdrawalId submit(@RequestBody SubmitWithdrawal request) {
    return new WithdrawalId(withdrawals.submit(request.userId(), request.amount(), request.beneficiaryRef()));
  }

  @PostMapping(FundingPaths.FIAT_SETTLEMENTS)
  public void onFiatSettled(@RequestBody FiatSettled request) {
    coordinator.onFiatSettled(request.userId(), request.amount(), request.providerReference());
  }
}
