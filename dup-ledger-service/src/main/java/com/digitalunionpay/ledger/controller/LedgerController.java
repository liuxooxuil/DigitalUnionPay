package com.digitalunionpay.ledger.controller;

import com.digitalunionpay.ledger.FreezeId;
import com.digitalunionpay.ledger.FreezeRequest;
import com.digitalunionpay.ledger.JournalCommand;
import com.digitalunionpay.ledger.LedgerPaths;
import com.digitalunionpay.ledger.service.LedgerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class LedgerController {

  private final LedgerService ledger;

  /** 接入本进程的服务入口。 */
  public LedgerController(LedgerService ledger) {
    this.ledger = ledger;
  }

  /** 受理一笔预冻结。 */
  @PostMapping(LedgerPaths.FREEZES)
  public FreezeId tryFreeze(@RequestBody FreezeRequest request) {
    return ledger.tryFreeze(request);
  }

  /** 按冻结编号确认入账。 */
  @PostMapping(LedgerPaths.FREEZE_CONFIRM)
  public void confirm(@PathVariable("id") String id) {
    ledger.confirm(new FreezeId(id));
  }

  /** 按冻结编号回滚预冻结。 */
  @PostMapping(LedgerPaths.FREEZE_CANCEL)
  public void cancel(@PathVariable("id") String id) {
    ledger.cancel(new FreezeId(id));
  }

  /** 受理一笔复式分录。 */
  @PostMapping(LedgerPaths.JOURNALS)
  public void post(@RequestBody JournalCommand command) {
    ledger.post(command);
  }
}
