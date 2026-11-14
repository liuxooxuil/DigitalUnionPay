package com.digitalunionpay.ledger;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LedgerController {

  private final TccLedger ledger;

  public LedgerController(TccLedger ledger) {
    this.ledger = ledger;
  }

  @PostMapping(LedgerPaths.FREEZES)
  public FreezeId tryFreeze(@RequestBody FreezeRequest request) {
    return ledger.tryFreeze(request);
  }

  @PostMapping(LedgerPaths.FREEZE_CONFIRM)
  public void confirm(@PathVariable("id") String id) {
    ledger.confirm(new FreezeId(id));
  }

  @PostMapping(LedgerPaths.FREEZE_CANCEL)
  public void cancel(@PathVariable("id") String id) {
    ledger.cancel(new FreezeId(id));
  }

  @PostMapping(LedgerPaths.JOURNALS)
  public void post(@RequestBody JournalCommand command) {
    ledger.post(command);
  }
}
