package com.digitalunionpay.user.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.AccountBalance;
import com.digitalunionpay.ledger.StatementLine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 测试剖面不连接账本进程。余额记在内存里，开户时是 0。 */
@Component
@Profile("test")
public class LocalCustomerLedger implements CustomerLedger {

  private final ConcurrentHashMap<String, long[]> rows = new ConcurrentHashMap<>();

  private final List<Noted> notes = Collections.synchronizedList(new ArrayList<>());

  /** 没有行时放入 0。已有行保持不变。 */
  @Override
  public void openZero(String accountId, String currency, int scale, String ownerUserId) {
    Ids.required(accountId, "accountId");
    Ids.required(currency, "currency");
    Ids.required(ownerUserId, "ownerUserId");
    rows.putIfAbsent(key(ownerUserId, accountId, currency), new long[] {0, 0, scale});
  }

  /** 列出这个用户名下已经开过的账户。 */
  @Override
  public List<AccountBalance> balances(String ownerUserId) {
    Ids.required(ownerUserId, "ownerUserId");
    List<AccountBalance> found = new ArrayList<>();
    String prefix = ownerUserId + "|";
    rows.forEach((id, amounts) -> {
      if (id.startsWith(prefix)) {
        String[] parts = id.split("\\|", 3);
        found.add(new AccountBalance(parts[1], parts[2], amounts[0], amounts[1], (int) amounts[2]));
      }
    });
    return found;
  }

  /** 返回这个用户在转账时记下的流水。from、to 是 UTC 时刻，包含两端。 */
  @Override
  public List<StatementLine> statements(String ownerUserId, String from, String to, String side) {
    Ids.required(ownerUserId, "ownerUserId");
    Instant start = from == null || from.isBlank() ? null : Instant.parse(from);
    Instant end = to == null || to.isBlank() ? null : Instant.parse(to);
    List<StatementLine> found = new ArrayList<>();
    synchronized (notes) {
      for (Noted noted : notes) {
        if (!ownerUserId.equals(noted.owner)) {
          continue;
        }
        if (side != null && !side.isBlank() && !side.equals(noted.line.side())) {
          continue;
        }
        Instant at = Instant.parse(noted.line.createdAt());
        if (start != null && at.isBefore(start)) {
          continue;
        }
        if (end != null && at.isAfter(end)) {
          continue;
        }
        found.add(noted.line);
      }
    }
    return found;
  }

  /** 可用余额不足时拒绝。转入账户不存在时按 0 开立后再增加。 */
  @Override
  public void transfer(
      String reference, String fromAccountId, String toAccountId, String currency, int scale, long minorUnits) {
    Ids.required(reference, "reference");
    Ids.required(fromAccountId, "fromAccountId");
    Ids.required(toAccountId, "toAccountId");
    if (minorUnits == 0) {
      return;
    }
    if (minorUnits < 0) {
      throw new IllegalArgumentException("minorUnits");
    }
    String fromKey = find(fromAccountId, currency);
    long[] source = rows.get(fromKey);
    if (source == null || source[0] < minorUnits) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, fromAccountId);
    }
    source[0] -= minorUnits;
    String toKey = find(toAccountId, currency);
    rows.compute(toKey, (id, amounts) -> {
      long[] next = amounts == null ? new long[] {0, 0, scale} : amounts;
      next[0] += minorUnits;
      return next;
    });
    String at = Instant.now().toString();
    notes.add(new Noted(owner(fromKey), new StatementLine(reference, fromAccountId, "DEBIT", minorUnits, scale, currency, at)));
    notes.add(new Noted(owner(toKey), new StatementLine(reference, toAccountId, "CREDIT", minorUnits, scale, currency, at)));
  }

  /** 测试把可用余额改成给定值。没有行时先开立。 */
  public void setAvailable(String ownerUserId, String accountId, String currency, int scale, long available) {
    rows.put(key(ownerUserId, accountId, currency), new long[] {available, 0, scale});
  }

  /** 按账户和币种找回带归属人的键。没有时用空归属人。 */
  private String find(String accountId, String currency) {
    String suffix = "|" + accountId + "|" + currency;
    for (String id : rows.keySet()) {
      if (id.endsWith(suffix)) {
        return id;
      }
    }
    return key("", accountId, currency);
  }

  /** 归属人、账户和币种拼成一个键。 */
  private static String key(String ownerUserId, String accountId, String currency) {
    return ownerUserId + "|" + accountId + "|" + currency;
  }

  /** 键的第一段是归属人。 */
  private static String owner(String key) {
    int cut = key.indexOf('|');
    return cut < 0 ? "" : key.substring(0, cut);
  }

  /** 一条带归属人的流水。 */
  private record Noted(String owner, StatementLine line) {
  }
}
