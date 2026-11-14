package com.digitalunionpay.ledger;

import org.springframework.web.client.RestClient;

public final class HttpTccLedger implements TccLedger {
  private final RestClient client;

  public HttpTccLedger(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  @Override
  public FreezeId tryFreeze(FreezeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request");
    }
    return body(client.post().uri(LedgerPaths.FREEZES).body(request).retrieve().body(FreezeId.class), "freezeId");
  }

  @Override
  public void confirm(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    client.post().uri(LedgerPaths.FREEZE_CONFIRM, freezeId.value()).retrieve().toBodilessEntity();
  }

  @Override
  public void cancel(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    client.post().uri(LedgerPaths.FREEZE_CANCEL, freezeId.value()).retrieve().toBodilessEntity();
  }

  @Override
  public void post(JournalCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    client.post().uri(LedgerPaths.JOURNALS).body(command).retrieve().toBodilessEntity();
  }
}
