package com.digitalunionpay.ledger;

/** 一条账本流水。createdAt 是 UTC，旧分录可能为空。 */
public record StatementLine(
    String reference,
    String accountId,
    String side,
    long minorUnits,
    int scale,
    String currency,
    String createdAt) {
}
