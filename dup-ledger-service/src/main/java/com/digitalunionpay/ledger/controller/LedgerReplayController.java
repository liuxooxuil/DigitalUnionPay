package com.digitalunionpay.ledger.controller;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.support.Outbox;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 把应用死信重新放回本地消息表。业务是否完成仍以账本为准，不靠这条补偿本身。 */
@RestController
public class LedgerReplayController {

  private final JdbcTemplate jdbc;

  private final Outbox outbox;

  /** 接入死信表和本地消息表。 */
  public LedgerReplayController(JdbcTemplate jdbc, Outbox outbox) {
    this.jdbc = jdbc;
    this.outbox = outbox;
  }

  /** 找不到死信时返回冲突。重新入队后删掉原死信。 */
  @PostMapping("/internal/ops/dead-letters/{id}/replay")
  public void replay(@PathVariable("id") String id) {
    Letter letter = jdbc.query(
        "SELECT event_type, payload FROM dup_dead_letter WHERE id = ?",
        rs -> rs.next() ? new Letter(rs.getString(1), rs.getString(2)) : null,
        id);
    if (letter == null) {
      throw new BusinessException(ErrorCode.CONFLICT, id);
    }
    outbox.append(letter.type, letter.payload.isBlank() ? id : letter.payload, "replay-" + id);
    jdbc.update("DELETE FROM dup_dead_letter WHERE id = ?", id);
  }

  /** 一条待补偿的死信。 */
  private record Letter(String type, String payload) {
  }
}
