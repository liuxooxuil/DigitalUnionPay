package com.digitalunionpay.ledger;

public final class LedgerPaths {
  public static final String FREEZES = "/internal/ledger/freezes";
  public static final String FREEZE_CONFIRM = "/internal/ledger/freezes/{id}/confirm";
  public static final String FREEZE_CANCEL = "/internal/ledger/freezes/{id}/cancel";
  public static final String JOURNALS = "/internal/ledger/journals";

  private LedgerPaths() {
  }
}
