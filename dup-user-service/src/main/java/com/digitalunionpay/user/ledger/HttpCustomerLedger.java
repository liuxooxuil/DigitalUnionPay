package com.digitalunionpay.user.ledger;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.ledger.AccountBalance;
import com.digitalunionpay.ledger.EntrySide;
import com.digitalunionpay.ledger.JournalCommand;
import com.digitalunionpay.ledger.LedgerPaths;
import com.digitalunionpay.ledger.OpenAccount;
import com.digitalunionpay.ledger.Posting;
import com.digitalunionpay.ledger.StatementLine;
import com.digitalunionpay.support.ServiceNames;
import com.digitalunionpay.support.TracingRestClients;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 通过账本服务开户、查余额和转账。测试剖面不加载这个客户端。 */
@Component
@Profile("!test")
public class HttpCustomerLedger implements CustomerLedger {

  private final RestClient client;

  /** 指向 ledger-service。 */
  public HttpCustomerLedger(RestClient.Builder builder) {
    this.client = TracingRestClients.create(builder, ServiceNames.http(ServiceNames.LEDGER));
  }

  /** 调用开户接口。账本已有行时不会把余额改成别的数。 */
  @Override
  public void openZero(String accountId, String currency, int scale, String ownerUserId) {
    client.post().uri(LedgerPaths.ACCOUNTS).body(new OpenAccount(accountId, currency, scale, ownerUserId)).retrieve().toBodilessEntity();
  }

  /** 查询这个用户名下的余额。空响应当成没有账户。 */
  @Override
  public List<AccountBalance> balances(String ownerUserId) {
    Ids.required(ownerUserId, "ownerUserId");
    AccountBalance[] body = client.get()
        .uri(uri -> uri.path(LedgerPaths.ACCOUNTS).queryParam("ownerUserId", ownerUserId).build())
        .retrieve()
        .body(AccountBalance[].class);
    return body == null ? List.of() : Arrays.asList(body);
  }

  /** 按时间和借贷方向查询流水。 */
  @Override
  public List<StatementLine> statements(String ownerUserId, String from, String to, String side) {
    Ids.required(ownerUserId, "ownerUserId");
    StatementLine[] body = client.get()
        .uri(uri -> uri.path(LedgerPaths.STATEMENTS)
            .queryParam("ownerUserId", ownerUserId)
            .queryParamIfPresent("from", java.util.Optional.ofNullable(blank(from)))
            .queryParamIfPresent("to", java.util.Optional.ofNullable(blank(to)))
            .queryParamIfPresent("side", java.util.Optional.ofNullable(blank(side)))
            .build())
        .retrieve()
        .body(StatementLine[].class);
    return body == null ? List.of() : Arrays.asList(body);
  }

  /** 借原账户、贷继承人账户，两笔记账在账本里合成一笔平衡分录。 */
  @Override
  public void transfer(
      String reference, String fromAccountId, String toAccountId, String currency, int scale, long minorUnits) {
    Ids.required(reference, "reference");
    if (minorUnits == 0) {
      return;
    }
    Money money = new Money(minorUnits, scale, currency);
    JournalCommand command = new JournalCommand(
        reference,
        reference,
        List.of(
            new Posting(fromAccountId, EntrySide.DEBIT, money),
            new Posting(toAccountId, EntrySide.CREDIT, money)));
    client.post().uri(LedgerPaths.JOURNALS).body(command).retrieve().toBodilessEntity();
  }

  /** 空白查询参数不往外传。 */
  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
