package com.digitalunionpay.user.ledger;

import com.digitalunionpay.ledger.AccountBalance;
import com.digitalunionpay.ledger.StatementLine;
import java.util.List;

/** 用户服务看账本。余额只在账本里变，这里只开户和查询。 */
public interface CustomerLedger {

  /** 开立可用和冻结都为 0 的账户。 */
  void openZero(String accountId, String currency, int scale, String ownerUserId);

  /** 这个用户名下的余额。 */
  List<AccountBalance> balances(String ownerUserId);

  /** 这个用户名下的流水。from、to、side 可空。 */
  List<StatementLine> statements(String ownerUserId, String from, String to, String side);

  /** 把一笔可用余额从原账户转到继承人账户。金额为 0 时不记账。 */
  void transfer(String reference, String fromAccountId, String toAccountId, String currency, int scale, long minorUnits);
}
