package com.digitalunionpay.ledger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalCommandTest {

  @Test
  void acceptsBalancedPostingsPerCurrency() {
    JournalCommand command = new JournalCommand(
        "trace-1",
        "ref-1",
        List.of(
            new Posting("customer-myr", EntrySide.DEBIT, new Money(1000, 2, "MYR")),
            new Posting("nostro-myr", EntrySide.CREDIT, new Money(1000, 2, "MYR"))));

    assertDoesNotThrow(command::requireBalanced);
  }

  @Test
  void rejectsUnbalancedPostings() {
    JournalCommand command = new JournalCommand(
        "trace-1",
        "ref-1",
        List.of(
            new Posting("customer-myr", EntrySide.DEBIT, new Money(1000, 2, "MYR")),
            new Posting("nostro-myr", EntrySide.CREDIT, new Money(900, 2, "MYR"))));

    BusinessException error = assertThrows(BusinessException.class, command::requireBalanced);
    assertEquals(ErrorCode.UNBALANCED_JOURNAL, error.code());
  }
}
