# DigitalUnionPay

A platform for digital assets and fiat funds. Version `0.1.0-SNAPSHOT` is a service skeleton: module boundaries, internal APIs, and processes are already split. Each business port validates its input, then returns `PORT_NOT_IMPLEMENTED`.

Customer balances change only inside the ledger. Other services call the ledger over HTTP.

## Stack

| Item | Version |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.0.9 |
| Spring Cloud | 2025.1.4 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Registry and config | Nacos |
| Rate limiting | Sentinel |
| Distributed transactions | Seata |
| Messaging | RocketMQ |

The parent project is `com.digitalunionpay:digital-union-pay`. Each business domain is split into two modules:

- `*-api`: the contract. Records, enums, port interfaces, and HTTP clients.
- `*-service`: a standalone Spring Boot process. Controllers hand requests to the port implementation.

## Modules

| Modules | Process | Port | Responsibility |
| --- | --- | --- | --- |
| `dup-common` | — | — | Money, error codes, and identifier checks. No business flow |
| `dup-service-support` | — | — | Trace IDs, Nacos, Sentinel, Seata, RocketMQ, and traced HTTP clients |
| `dup-user-api` / `dup-user-service` | user-service | 8081 | Registration, sessions, KYC, account status, and inheritance. Balances stay in the ledger |
| `dup-ledger-api` / `dup-ledger-service` | ledger-service | 8082 | Double-entry journals and TCC. The only entry point for balance changes |
| `dup-risk-api` / `dup-risk-service` | risk-service | 8083 | Limit and sanctions checks before deposit, withdrawal, exchange, and trading |
| `dup-valuation-api` / `dup-valuation-service` | valuation-service | 8084 | FX rates, gold prices, and crypto quotes. Read only |
| `dup-notify-api` / `dup-notify-service` | notify-service | 8085 | In-app inbox, SMS, email, and app push |
| `dup-funding-api` / `dup-funding-service` | funding-service | 8086 | Fiat deposits and withdrawals. Virtual IBANs and withdrawal requests |
| `dup-exchange-api` / `dup-exchange-service` | exchange-service | 8087 | Fiat and stablecoin exchange. Two-sided posting after a price lock |
| `dup-matching-api` / `dup-matching-service` | matching-service | 8088 | Order book. Settlement of a fill goes to the ledger |
| `dup-crypto-api` / `dup-crypto-service` | crypto-service | 8089 | Deposit addresses, confirmations, and custody signing |
| `dup-physical-api` / `dup-physical-service` | physical-service | 8090 | Physical intake. Device readings and RFID |
| `dup-trading-api` / `dup-trading-service` | trading-service | 8091 | Proprietary trading. MT5 gateway; margin and P&L post through the ledger |

Service names live in `com.digitalunionpay.support.ServiceNames` and match each process `spring.application.name`. Calls between services use addresses such as `http://ledger-service`, resolved by Nacos and the load balancer.

## Boundaries

- `TccLedger` is the only posting entry point: `tryFreeze` reserves funds, `confirm` posts them, `cancel` rolls the reservation back, and `post` posts a journal directly.
- `post` accepts a journal only when it is already balanced. For each currency, debit minor units must equal credit minor units, and every posting of that currency in the journal must use the same `scale`.
- Fiat settlement enters through `FundingCoordinator.onFiatSettled`. The order is KYC, then risk, then the ledger TCC. Exchange stays in its own module.
- Valuation returns quotes only.
- The user module owns KYC and inheritance cases. After an inheritance case is approved, the ledger moves the funds.
- Crypto custody signing is a 3-of-5 threshold signature plus a second confirmation on an HSM. Customer balances still post through the ledger.
- Callers submit a notification event and leave channel delivery to the notify service. Cross-process notices go through RocketMQ on topic `digitalunionpay-domain-events`.

## Money

`Money` stores an integer in minor units so amounts never use floating point.

- `minorUnits` must be greater than or equal to 0.
- `scale` is from 0 to 18.
- `currency` is 3 to 10 uppercase letters or digits.

Identifier fields go through `Ids.required`. Null and blank values are rejected before business logic runs.

## States

| Domain | Values |
| --- | --- |
| User | `PENDING_KYC` → `ACTIVE` / `FROZEN` / `CLOSED` / `INHERITED` |
| KYC | `NOT_SUBMITTED` → `IN_REVIEW` → `APPROVED` / `REJECTED` / `RFI`. Only `APPROVED` allows funding |
| Account | `NORMAL` / `FROZEN` / `CLOSED` |
| Inheritance | `PENDING` → `UNDER_REVIEW` → `FROZEN` → `COMPLETED` / `REJECTED` |
| Fiat deposit | `PENDING_CONFIRM` → `RECEIVED` → `POSTED` / `EXCEPTION` / `REFUND` |
| Withdrawal | `APPLIED` → `IN_REVIEW` → `PROCESSING` → `SUCCEEDED` / `FAILED` |
| Journal side | `DEBIT` / `CREDIT` |
| Risk action | `DEPOSIT` / `WITHDRAW` / `EXCHANGE` / `TRADE` |
| Risk level | `LOW` / `MEDIUM` / `HIGH` |
| Notify event | `DEPOSIT_POSTED`, `EXCHANGE_RESULT`, `WITHDRAWAL_RESULT`, `RISK_BLOCKED`, `KYC_RESULT`, `INHERITANCE_PROGRESS`, `MARGIN_CALL` |
| Notify channel | `INBOX` / `SMS` / `EMAIL` / `PUSH` |

There are two error codes: `UNBALANCED_JOURNAL` and `PORT_NOT_IMPLEMENTED`.

## Internal APIs

These paths are for calls between services. Every path starts with `/internal`.

| Service | Method and path | Purpose |
| --- | --- | --- |
| User | `GET /internal/users/{userId}/funding-allowed` | Check that KYC is approved before funding or exchange |
| User | `POST /internal/inheritance-cases` | Open an inheritance case |
| Ledger | `POST /internal/ledger/freezes` | TCC Try. Idempotent reserve by reference |
| Ledger | `POST /internal/ledger/freezes/{id}/confirm` | TCC Confirm |
| Ledger | `POST /internal/ledger/freezes/{id}/cancel` | TCC Cancel |
| Ledger | `POST /internal/ledger/journals` | Submit a balanced double-entry journal |
| Risk | `POST /internal/risk/checks` | Synchronous gate |
| Valuation | `POST /internal/valuation/conversions` | Quote from a source amount into a target currency |
| Notify | `POST /internal/notifications` | Submit one notification |
| Funding | `POST /internal/funding/virtual-ibans` | Issue a virtual IBAN |
| Funding | `POST /internal/funding/withdrawals` | Submit a withdrawal |
| Funding | `POST /internal/funding/fiat-settlements` | Record fiat settlement |
| Exchange | `POST /internal/exchange/locks` | Lock a price |
| Matching | `POST /internal/matching/orders` | Accept an order |
| Crypto | `POST /internal/crypto/deposit-addresses` | Allocate a deposit address |
| Crypto | `POST /internal/crypto/signatures` | Custody signature |
| Physical | `POST /internal/physical/intakes` | Record one physical intake |
| Trading | `POST /internal/trading/market-orders` | Open a market order |

When an inbound request has no `X-Trace-Id`, the filter creates a UUID, stores it in the MDC, and writes it back on the response. Outbound HTTP calls keep that same trace ID. The log level pattern is `%5p [trace:%X{X-Trace-Id:-}]`.

Health is exposed at `/actuator/health`.

## Runtime dependencies

Any profile other than `test` imports `classpath:digitalunionpay-nacos.yml` and the optional `nacos:<service-name>.yaml`.

| Variable | Default | Purpose |
| --- | --- | --- |
| `NACOS_ADDR` | `127.0.0.1:8848` | Registry and config server |
| `NACOS_USERNAME` | `nacos` | Nacos username |
| `NACOS_PASSWORD` | `nacos` | Nacos password |
| `SENTINEL_DASHBOARD` | `127.0.0.1:8080` | Sentinel dashboard |
| `ROCKETMQ_NAMESRV` | `127.0.0.1:9876` | RocketMQ NameServer |
| `SEATA_ADDR` | `127.0.0.1:7091` | Seata transaction coordinator |

The Seata transaction group is `digitalunionpay-tx-group`, registered in Nacos group `SEATA_GROUP`. The data-source proxy is off by default (`enable-auto-data-source-proxy: false`).

The `test` profile turns off Nacos, Sentinel, Seata, and the RocketMQ health check so context tests can start.

## Build and test

From the repository root:

```bash
mvn test
```

Build one service and the modules it depends on:

```bash
mvn -pl dup-ledger-service -am test
```

Start one process. The infrastructure in the table above must be running, or Nacos must already hold the service config:

```bash
mvn -pl dup-ledger-service spring-boot:run
```

Service tests currently check that the Spring context starts under the `test` profile. Business ports are not yet wired to a real channel, a ledger store, or an external price feed.

## Current state

Each service keeps a `Skeleton*` implementation in its `internal` package. The skeleton checks for null input and for collaborators that were not injected, then throws `BusinessException(PORT_NOT_IMPLEMENTED)`. Ledger `post` calls `JournalCommand.requireBalanced()` first, so an unbalanced journal is rejected before any store exists.

Later implementations should replace these skeletons and keep the current ports and paths.
