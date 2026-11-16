package com.digitalunionpay.ledger;

import org.springframework.web.client.RestClient;

/** 调用清结算服务 8082 的 TccLedger。 */
public final class HttpTccLedger implements TccLedger {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpTccLedger(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  /** 响应体为空时失败，避免把空结果当成成功。 */
  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  /** 调用 POST /internal/ledger/freezes 并取回冻结编号。 */
  @Override
  public FreezeId tryFreeze(FreezeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request");
    }
    return body(client.post().uri(LedgerPaths.FREEZES).body(request).retrieve().body(FreezeId.class), "freezeId");
  }

  /** 调用 POST /internal/ledger/freezes/{id}/confirm 确认预冻结。 */
  @Override
  public void confirm(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    client.post().uri(LedgerPaths.FREEZE_CONFIRM, freezeId.value()).retrieve().toBodilessEntity();
  }

  /** 调用 POST /internal/ledger/freezes/{id}/cancel 回滚预冻结。 */
  @Override
  public void cancel(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    client.post().uri(LedgerPaths.FREEZE_CANCEL, freezeId.value()).retrieve().toBodilessEntity();
  }

  /** 调用 POST /internal/ledger/journals 提交分录。 */
  @Override
  public void post(JournalCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    client.post().uri(LedgerPaths.JOURNALS).body(command).retrieve().toBodilessEntity();
  }
}
