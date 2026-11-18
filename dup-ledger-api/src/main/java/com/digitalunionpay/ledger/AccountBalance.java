package com.digitalunionpay.ledger;

/** 一个账本账户的可用和冻结余额，单位是最小货币单位。 */
public record AccountBalance(String accountId, String currency, long available, long frozen, int scale) {
}
