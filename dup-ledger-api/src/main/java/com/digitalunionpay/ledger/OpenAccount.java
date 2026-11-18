package com.digitalunionpay.ledger;

/** 开立一个余额为 0 的账本账户。已存在时保持原余额。 */
public record OpenAccount(String accountId, String currency, int scale, String ownerUserId) {
}
