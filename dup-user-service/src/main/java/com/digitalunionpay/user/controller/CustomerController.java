package com.digitalunionpay.user.controller;

import com.digitalunionpay.ledger.StatementLine;
import com.digitalunionpay.user.UserPaths;
import com.digitalunionpay.user.auth.CurrentUser;
import com.digitalunionpay.user.service.CustomerForms;
import com.digitalunionpay.user.service.CustomerViews;
import com.digitalunionpay.user.service.KycService;
import com.digitalunionpay.user.service.impl.AccountService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户的 KYC、余额、限额、收款账户和关闭申请。 */
@RestController
public class CustomerController {

  private final CurrentUser current;

  private final KycService kyc;

  private final AccountService accounts;

  /** 接入当前用户、KYC 和账户。 */
  public CustomerController(CurrentUser current, KycService kyc, AccountService accounts) {
    this.current = current;
    this.kyc = kyc;
    this.accounts = accounts;
  }

  /** 档案和账户状态。 */
  @GetMapping(UserPaths.ME)
  public CustomerViews.AccountSnapshot me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return accounts.snapshot(current.require(authorization).userId());
  }

  /** 法币和加密子账户余额。 */
  @GetMapping(UserPaths.ME_BALANCES)
  public List<CustomerViews.Holding> balances(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return accounts.balances(current.require(authorization).userId());
  }

  /** 按时间和借贷方向查流水。type 与 side 相同。 */
  @GetMapping(UserPaths.ME_STATEMENTS)
  public List<StatementLine> statements(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "to", required = false) String to,
      @RequestParam(value = "side", required = false) String side,
      @RequestParam(value = "type", required = false) String type) {
    String direction = side == null || side.isBlank() ? type : side;
    return accounts.statements(current.require(authorization).userId(), from, to, direction);
  }

  /** 按 KYC 等级检查这一笔是否超过单笔、日累计或月累计。 */
  @PostMapping(UserPaths.ME_LIMITS)
  public CustomerViews.LimitView limits(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.LimitCheck form) {
    return accounts.checkLimit(current.require(authorization).userId(), form);
  }

  /** 绑定同名收款账户。 */
  @PostMapping(UserPaths.ME_BANK)
  public CustomerViews.BankAccountView bank(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.BindBank form) {
    return accounts.bindBank(current.require(authorization).userId(), form);
  }

  /** 余额必须为 0 才能申请关闭。 */
  @PostMapping(UserPaths.ME_CLOSURE)
  public CustomerViews.ClosureView closure(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return accounts.requestClosure(current.require(authorization).userId());
  }

  /** 人工审核关闭申请。 */
  @PostMapping(UserPaths.CLOSURE_DECISION)
  public CustomerViews.ClosureView decideClosure(
      @PathVariable("userId") String userId,
      @RequestBody CustomerForms.ClosureDecision form) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    return accounts.decideClosure(userId, form.approve());
  }

  /** 上传证件的一面。 */
  @PostMapping(UserPaths.ME_KYC_DOCUMENTS)
  public CustomerViews.DocumentReceipt upload(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.DocumentUpload form) {
    return kyc.upload(current.require(authorization).userId(), form);
  }

  /** 识别证件文本。识别不了就返回 recognized=false。 */
  @PostMapping(UserPaths.ME_KYC_OCR)
  public CustomerViews.OcrResult ocr(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.OcrRequest form) {
    current.require(authorization);
    return kyc.ocr(form);
  }

  /** 签发活体会话。 */
  @PostMapping(UserPaths.ME_KYC_LIVENESS)
  public CustomerViews.LivenessTicket liveness(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return kyc.liveness(current.require(authorization).userId());
  }

  /** 用户确认后提交 KYC。 */
  @PostMapping(UserPaths.ME_KYC)
  public CustomerViews.KycView submit(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody CustomerForms.KycSubmit form) {
    return kyc.submit(current.require(authorization).userId(), form);
  }

  /** 查看最新 KYC。 */
  @GetMapping(UserPaths.ME_KYC)
  public CustomerViews.KycView kyc(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return kyc.current(current.require(authorization).userId());
  }

  /** 审核员给出通过、拒绝或补充材料。 */
  @PostMapping(UserPaths.KYC_DECISION)
  public CustomerViews.KycView decide(
      @PathVariable("userId") String userId,
      @RequestBody CustomerForms.KycDecision form) {
    return kyc.decide(userId, form);
  }
}
