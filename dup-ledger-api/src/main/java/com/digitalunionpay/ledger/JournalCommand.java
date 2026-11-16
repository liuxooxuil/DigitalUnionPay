package com.digitalunionpay.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一笔复式分录。同一币种的借方最小单位合计必须等于贷方。
 */
public record JournalCommand(String traceId, String reference, List<Posting> postings) {

  /** 拒绝空白链路号和参考号，且分录至少两条。 */
  public JournalCommand {
    traceId = Ids.required(traceId, "traceId");
    reference = Ids.required(reference, "reference");
    if (postings == null || postings.size() < 2) {
      throw new IllegalArgumentException("a journal needs at least two postings");
    }
    postings = List.copyOf(postings);
  }

  /** 同一币种借贷必须相抵，scale 不一致也视为不平衡。 */
  public void requireBalanced() {
    Map<String, Integer> scales = new HashMap<>();
    Map<String, Long> net = new HashMap<>();
    for (Posting posting : postings) {
      String currency = posting.amount().currency();
      int scale = posting.amount().scale();
      Integer existingScale = scales.putIfAbsent(currency, scale);
      if (existingScale != null && existingScale != scale) {
        throw new BusinessException(ErrorCode.UNBALANCED_JOURNAL, currency);
      }
      long signed = posting.side() == EntrySide.DEBIT
          ? posting.amount().minorUnits()
          : -posting.amount().minorUnits();
      net.merge(currency, signed, Long::sum);
    }
    for (Map.Entry<String, Long> entry : net.entrySet()) {
      if (entry.getValue() != 0L) {
        throw new BusinessException(ErrorCode.UNBALANCED_JOURNAL, entry.getKey());
      }
    }
  }
}
