package com.digitalunionpay.ledger.service;

import com.digitalunionpay.ledger.AccountBalance;
import com.digitalunionpay.ledger.OpenAccount;
import com.digitalunionpay.ledger.StatementLine;
import com.digitalunionpay.ledger.TccLedger;
import java.util.List;

/** 这是 TccLedger 的服务入口。开户和查询不改变业务分录规则。 */
public interface LedgerService extends TccLedger {

  /** 没有账户行时插入可用和冻结都为 0 的行。已有行不改余额。 */
  void openAccount(OpenAccount request);

  /** 按归属人列出余额。 */
  List<AccountBalance> balances(String ownerUserId);

  /** 按归属人、时间和借贷方向列出流水。from、to、side 可空。 */
  List<StatementLine> statements(String ownerUserId, String from, String to, String side);
}
