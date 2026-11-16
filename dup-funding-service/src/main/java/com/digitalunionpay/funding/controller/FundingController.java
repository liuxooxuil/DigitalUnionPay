package com.digitalunionpay.funding.controller;

import com.digitalunionpay.funding.FiatSettled;
import com.digitalunionpay.funding.FundingPaths;
import com.digitalunionpay.funding.IssueVirtualIban;
import com.digitalunionpay.funding.IssuedIban;
import com.digitalunionpay.funding.SubmitWithdrawal;
import com.digitalunionpay.funding.WithdrawalId;
import com.digitalunionpay.funding.service.FiatCollectionService;
import com.digitalunionpay.funding.service.FundingSettleService;
import com.digitalunionpay.funding.service.WithdrawalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class FundingController {

  private final FiatCollectionService fiat;
  private final WithdrawalService withdrawals;
  private final FundingSettleService coordinator;

  /** 接入本进程的端口实现。 */
  public FundingController(
      FiatCollectionService fiat, WithdrawalService withdrawals, FundingSettleService coordinator) {
    this.fiat = fiat;
    this.withdrawals = withdrawals;
    this.coordinator = coordinator;
  }

  /** 签发虚拟 IBAN。 */
  @PostMapping(FundingPaths.VIRTUAL_IBANS)
  public IssuedIban issueVirtualIban(@RequestBody IssueVirtualIban request) {
    return new IssuedIban(fiat.issueVirtualIban(request.userId(), request.currency()));
  }

  /** 提交出金申请。 */
  @PostMapping(FundingPaths.WITHDRAWALS)
  public WithdrawalId submit(@RequestBody SubmitWithdrawal request) {
    return new WithdrawalId(withdrawals.submit(request.userId(), request.amount(), request.beneficiaryRef()));
  }

  /** 通知法币到账。 */
  @PostMapping(FundingPaths.FIAT_SETTLEMENTS)
  public void onFiatSettled(@RequestBody FiatSettled request) {
    coordinator.onFiatSettled(request.userId(), request.amount(), request.providerReference());
  }
}
