# DigitalUnionPay

A platform for digital assets and fiat funds. Version `0.1.0-SNAPSHOT` splits module boundaries, internal APIs, and processes. Customer balances change only inside the ledger.

External callers enter through the edge gateway on port `8070`. They do not call ledger, funding, crypto, exchange, matching, trading, or physical services directly. Most business ports still validate input and return `PORT_NOT_IMPLEMENTED`. Ledger posting, user registration, user login, and the edge checks below are implemented.

## Stack

| Item | Version |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.0.9 |
| Spring Cloud | 2025.1.4 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Spring Cloud Gateway | 5.0.4, server MVC |
| Registry and config | Nacos |
| Rate limiting and degrade | Sentinel, in process |
| Distributed transactions | Seata |
| Messaging | RocketMQ |
| Schema | Flyway, one history table per service |

The parent project is `com.digitalunionpay:digital-union-pay`. Each business domain is split into two modules:

- `*-api`: the contract. Records, enums, port interfaces, and HTTP clients.
- `*-service`: a standalone Spring Boot process. Controllers hand requests to the port implementation.

## Modules

| Modules | Process | Port | Responsibility |
| --- | --- | --- | --- |
| `dup-common` | — | — | Money, error codes, and identifier checks. No business flow |
| `dup-service-support` | — | — | Trace IDs, security, Flyway platform tables, outbox, Sentinel, Seata, RocketMQ |
| `dup-gateway` | edge-gateway | 8070 | The only external HTTP entry. Nacos console uses 8080 |
| `dup-user-api` / `dup-user-service` | user-service | 8081 | Registration, sessions, RBAC, KYC, account status, and inheritance |
| `dup-ledger-api` / `dup-ledger-service` | ledger-service | 8082 | Double-entry journals, balances, and TCC |
| `dup-risk-api` / `dup-risk-service` | risk-service | 8083 | Limit and sanctions checks |
| `dup-valuation-api` / `dup-valuation-service` | valuation-service | 8084 | FX rates, gold prices, and crypto quotes. Read only |
| `dup-notify-api` / `dup-notify-service` | notify-service | 8085 | In-app inbox, SMS, email, and app push |
| `dup-funding-api` / `dup-funding-service` | funding-service | 8086 | Fiat deposits and withdrawals |
| `dup-exchange-api` / `dup-exchange-service` | exchange-service | 8087 | Fiat and stablecoin exchange |
| `dup-matching-api` / `dup-matching-service` | matching-service | 8088 | Order book |
| `dup-crypto-api` / `dup-crypto-service` | crypto-service | 8089 | Deposit addresses and custody signing |
| `dup-physical-api` / `dup-physical-service` | physical-service | 8090 | Physical intake |
| `dup-trading-api` / `dup-trading-service` | trading-service | 8091 | Proprietary trading |

Service names live in `com.digitalunionpay.support.ServiceNames`. Calls between services use addresses such as `http://ledger-service`.

## Edge gateway

`dup-gateway` is a Spring Cloud Gateway server MVC process. It does not use the JDBC, Flyway, Nacos, or Seata starters.

| Check | Behavior |
| --- | --- |
| Path isolation | `/internal/**` returns 403 `internal`. `/api/funding`, `/api/ledger`, `/api/crypto`, `/api/exchange`, `/api/matching`, `/api/trading`, and `/api/physical` return 403 `external`. Other external paths return 403 `external`, except the anonymous auth posts below, signed customer paths (`/api/users/**`, `/api/sessions/**`, `/api/auth/security/**`, `/api/inheritance-cases/**`, `/api/kyc-cases/**`, `/api/closures/**`), ping, and actuator |
| Register | `POST /api/auth/register` skips the edge signature and JWT. The gateway rewrites it to `POST /internal/auth/register`, forwards `X-Forwarded-For`, and adds service HMAC. Phone registration is an international dial code plus a 6-digit SMS code (5 minutes, 10 sends per phone per UTC day). Email registration uses a 6-digit code valid for 10 minutes. The code is not returned in the HTTP body. Password is 8–20 characters and must contain letters and digits. Success opens a main account in `PENDING_KYC` and a ledger account with available 0 and frozen 0. The same phone or email can register once. One IP can register at most 5 accounts per hour. If the user process is down, the gateway returns 503 `UNAVAILABLE` |
| Login | `POST /api/auth/login` skips the edge signature and JWT and is rewritten to `POST /internal/auth/login`. Five consecutive password failures lock the account for 30 minutes. SMS and email code login are `POST /api/auth/login/code`. App biometric login is `POST /api/auth/login/biometric` after `POST /api/auth/security/devices` binds an Ed25519 key. A missing login user still gets HTTP 200 from the code-send endpoint and no code is stored. Idle timeout defaults to 2 hours (`dup.auth.idle-hours`). The access token absolute lifetime stays 12 hours. At most 5 devices stay online. `GET /api/sessions` lists them and `DELETE /api/sessions/{id}` revokes one. A new device or a different IPv4 /16 sends an outbox alert. If the user process is down, the gateway returns 503 `UNAVAILABLE` |
| Ping | `GET /api/edge/ping` is local. It requires the edge HMAC and a JWT whose `grants` allow that method and path |
| Actuator | `/actuator/**` skips the edge signature and JWT so probes can run |
| IP deny list | `dup.edge.deny-ips` / `DUP_EDGE_DENY_IPS`. The first `X-Forwarded-For` hop is used, otherwise the socket address. Trust that header only when this process is the first public hop |
| User rate limit | Default 30 requests per 10 seconds, keyed by JWT subject. Login and registration are keyed by IP. In-memory on this process, not shared across replicas. HTTP 429 `RATE_LIMITED` |
| Edge HMAC | `X-Edge-Time`, `X-Edge-Nonce`, `X-Edge-Sign`. Canonical string is `method\npath\ntimestamp\nnonce\nhex(sha256(body))`. Secret `dup.edge.secret`, default `DigitalUnionPayEdgeSecretKey32!!`. This is not the service HMAC secret. Clock skew is 300 seconds. A repeated nonce returns 401 `nonce`. The nonce table is in-memory on this process |
| JWT | HS256, issuer `digitalunionpay`, secret `dup.auth.jwt-secret`. Missing or invalid tokens return 401 `token`. A valid token without the path grant returns 403 `path` |

## Security

| Area | What the code does |
| --- | --- |
| Human authentication | Spring Security on each business process. HS256 JWT, issuer `digitalunionpay`. Login stores `sha256(jti)` in `auth_token`, not the raw token. Access token absolute lifetime is 12 hours. A session also expires after 2 hours without use, which is `dup.auth.idle-hours`. At most 5 devices stay online. Claims include `uid` and `grants` (`METHOD path`) |
| Password storage | New passwords are Argon2id (`$argon2id$`). Existing PBKDF2 strings `iterations$salt$hash` still verify, including the seeded admin |
| Authorization | RBAC tables in the user service. Each process checks `grants` with an Ant path. Ledger also applies a narrow ABAC check: when the JWT `uid` and `ledger_account.owner_user_id` are both set, they must match |
| Service calls | HMAC headers `X-Service-Time`, `X-Service-Nonce`, `X-Service-Sign`, secret `dup.service-auth.secret`. Nonces are stored in `dup_service_nonce`. This is a shared service credential, not mTLS. No certificates are provisioned |
| Field encryption | `FieldCipher` is AES-256-GCM with a 32-byte key `dup.crypto.key`. The default is a local development key. It is not a KMS or an HSM |
| Audit | `AuditLog` inserts into `dup_audit` in the same local transaction as the ledger post or freeze |
| Custody signing | 3-of-5 threshold signing and an HSM second confirmation are design requirements. The crypto signature port still returns `PORT_NOT_IMPLEMENTED`. Those requirements are not a finished custody control |

The `test` profile sets `dup.auth.enabled=false` and `dup.service-auth.enabled=false`.

## Data

Local development uses one MySQL database, `digital_union_pay`, unless `MYSQL_URL` points somewhere else. Each service has its own Flyway history table, so schema changes do not share a version number. `CREATE TABLE IF NOT EXISTS` is used for platform tables because those services share one database. There is no sharding, no second physical database, and no backup or restore automation in this repository.

| Area | Tables | Notes |
| --- | --- | --- |
| Platform | `dup_idempotency`, `dup_outbox`, `dup_dead_letter`, `dup_tcc_fence`, `dup_service_nonce` | Created by `dup-service-support` |
| Audit and inbox | `dup_audit`, `dup_inbox` | Shared until each process gets its own database |
| User | `rbac_user`, `rbac_role`, `rbac_permission`, `rbac_user_role`, `rbac_role_permission`, `auth_token`, `user_profile`, `verification_code`, `registration_event`, `device_credential`, `kyc_case`, `kyc_document`, `sanctions_entry`, `airwallex_account`, `bank_account`, `kyc_limit`, `closure_request`, `inheritance_case` | Passwords live on `rbac_user`. Registration writes `user_profile` with `PENDING_KYC` and `NOT_SUBMITTED`, and the login row stays `ACTIVE`. KYC approval sets the user to `ACTIVE`, level `BASIC`, and queues an Airwallex connected account as `PENDING` when no base URL is configured. It does not mark that sync as completed. OFAC screening reads `sanctions_entry`; an empty table is not a hidden pass. Funding is allowed only when KYC is `APPROVED`, the user is `ACTIVE`, and the account is `NORMAL` |
| Funds | `fund`, `fund_account`, `user_fund_account` | One fund account per user per fund, like one card per fund. `user_fund_account` is unique on `(user_id, fund_code)` and on `account_no`. Registration inserts the `MAIN` fiat account and opens the same id in the ledger at zero. These tables hold no balances. Inheritance moves available balances with a balanced ledger journal |
| Ledger | `ledger_journal`, `ledger_posting`, `ledger_freeze`, `ledger_account` | The only balance store |
| Funding | `funding_deposit`, `funding_withdrawal` | Unique `reference`. The port still returns 501 and does not write these rows |
| Risk | `risk_decision` | Unique `reference`. Port still 501 |
| Orders | `match_order` | Unique `reference`. Port still 501 |
| Trades | `trade_fill` | Unique `reference`. Port still 501 |
| Exchange | `exchange_lock` | Unique `reference`. Port still 501 |

`ledger_account` is unique on `(account_id, currency)` and stores `available`, `frozen`, `scale`, `version`, and `owner_user_id` in integer minor units.

Ledger rules when a balance row exists:

1. A debit decreases `available`. A credit increases it. Optimistic `version` updates retry up to 5 times, then return 409 `version`.
2. `available < 0` rolls the whole transaction back with 422 `INSUFFICIENT_FUNDS`. Two concurrent debits cannot both spend the same available balance.
3. `ledger_journal.reference` is unique. The idempotency claim, journal, postings, balance update, audit row, and outbox row commit in one local transaction. The same business reference cannot post twice.
4. A freeze in `TRIED` moves `available` to `frozen`. Cancel moves it back. Confirm removes it from `frozen` and does not return it to `available`.
5. If no `ledger_account` row exists, a balanced journal still posts and does not invent a balance. Scale mismatch on an existing row is `UNBALANCED_JOURNAL`.

The outbox row is written in that same transaction. The relay sends it after commit. A committed journal whose message send fails stays unsent, retries, and after 5 failures is copied to `dup_dead_letter`. Database commit does not mean the message was delivered.

## Messaging

Topic `digitalunionpay-domain-events`. Binding `domainEvents-out-0`.

| Mechanism | Behavior |
| --- | --- |
| Outbox | `Outbox.append` only inserts. `OutboxRelay` polls every 2 seconds, 50 rows. `dup.outbox.publish=false` in tests, so tests do not call RocketMQ |
| Idempotent consume | Consumer bean `domainEvents` runs only when `spring.cloud.function.definition=domainEvents` (set in `digitalunionpay-nacos.yml`, not in the test profile). It dedupes on `Idempotency-Key` through `dup_inbox`, not on the random `eventId` |
| Retry and dead letter | Binder `max-attempts` is 3. The broker dead-letter destination is `%DLQ%digitalunionpay-domain-events`. The relay also writes `dup_dead_letter` and sets `sent_at` after 5 failures. An application dead letter can be requeued with `POST /internal/ops/dead-letters/{id}/replay`, which inserts another outbox row. That call is an internal operation, not an external API |
| Backlog | Gauge `dup.outbox.backlog` counts outbox rows with `sent_at IS NULL` |
| Order | Header `orderKey` is the idempotency key. The consumer is not an ordered RocketMQ consumer |
| Version | Header `eventVersion` is `1`. A higher version is parked with reason `version` and is not applied |
| Completion | A successful `StreamBridge.send` is not business completion. The ledger transaction is the business result. The message is a later notification |

## Observability

Implemented in the services that depend on `dup-service-support`:

- `/actuator/health` and `/actuator/prometheus`
- Micrometer tracing bridged to OpenTelemetry. Export stays off unless `OTLP_ENDPOINT` is set
- Trace header `X-Trace-Id`
- Sentinel inbound QPS default 200 and `user-service` QPS default 100. A blocked call returns 503 `UNAVAILABLE`. A degrade rule on resource `user-service` uses exception ratio 0.5, a 10 second window, and a minimum of 5 requests. The test profile turns Sentinel off
- Ledger gauges `dup.ledger.negative`, `dup.ledger.unbalanced`, and `dup.ledger.freeze_drift`
- Counters `dup.ledger.insufficient`, `dup.ledger.duplicate`, and `dup.db.slow`. The slow counter increments when the outbox poll or a ledger balance update takes at least 200 milliseconds. It does not time every SQL statement

`deploy/observability/` holds a Prometheus rule file, a Grafana dashboard JSON, and an OpenTelemetry collector config aimed at Jaeger. Those files are not deployed, and this repository does not run Prometheus, Grafana, Jaeger, SkyWalking, ELK, or Loki.

## Boundaries

- `TccLedger` is the posting entry point: `tryFreeze` reserves funds, `confirm` posts them, `cancel` releases them, and `post` posts a journal directly.
- `post` accepts a journal only when it is already balanced. For each currency, debit minor units must equal credit minor units, and every posting of that currency uses the same `scale`.
- Fiat settlement is meant to enter through `FundingCoordinator.onFiatSettled`. That coordinator is still a skeleton.
- Valuation returns quotes only.
- The user module owns KYC and inheritance cases. After an inheritance case is approved, the ledger moves the funds.
- Crypto custody signing remains a design requirement, as stated in the security table.
- Callers submit a notification event and leave channel delivery to the notify service.

## Money

`Money` stores an integer in minor units.

- `minorUnits` must be greater than or equal to 0.
- `scale` is from 0 to 18.
- `currency` is 3 to 10 uppercase letters or digits.

Identifier fields go through `Ids.required`.

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
| Freeze | `TRIED` / `CONFIRMED` / `CANCELLED` |
| Risk action | `DEPOSIT` / `WITHDRAW` / `EXCHANGE` / `TRADE` |
| Risk level | `LOW` / `MEDIUM` / `HIGH` |
| Notify event | `DEPOSIT_POSTED`, `EXCHANGE_RESULT`, `WITHDRAWAL_RESULT`, `RISK_BLOCKED`, `KYC_RESULT`, `INHERITANCE_PROGRESS`, `MARGIN_CALL` |
| Notify channel | `INBOX` / `SMS` / `EMAIL` / `PUSH` |

Failure bodies are `ApiResult(code, message, data, traceId)`. Successful bodies stay unwrapped. `PORT_NOT_IMPLEMENTED` is 501, `UNAUTHENTICATED` is 401, `FORBIDDEN` is 403, `BAD_REQUEST` and `VALIDATION` are 400, `CONFLICT` is 409, `RATE_LIMITED` is 429, `UNAVAILABLE` is 503. `INSUFFICIENT_FUNDS` and `UNBALANCED_JOURNAL` are 422.

## Internal APIs

These paths are for calls between services. The edge gateway rejects `/internal/**`.

| Service | Method and path | Purpose |
| --- | --- | --- |
| User | `POST /internal/auth/verification-codes` | Send a 6-digit SMS or email code. The body does not contain the code |
| User | `POST /internal/auth/register` | Create a login, a pending-KYC profile, and a zero-balance main account |
| User | `POST /internal/auth/login` | Exchange a username and password for a JWT. Five failures lock the account for 30 minutes |
| User | `POST /internal/auth/login/code` | Exchange a SMS or email code for a JWT |
| User | `POST /internal/auth/login/biometric` | Exchange a bound device signature for a JWT |
| User | `POST /internal/auth/access` | Re-check the token against the database, including idle timeout |
| User | `GET /internal/sessions` | List online devices |
| User | `DELETE /internal/sessions/{sessionId}` | Revoke one device |
| User | `GET /internal/users/{userId}/funding-allowed` | True only when KYC is approved, the user is active, and the account is normal |
| User | `POST /internal/users/me/kyc` | Submit confirmed KYC. A sanctions hit returns 403 `sanctions` and does not enter review |
| User | `POST /internal/kyc-cases/{userId}/decision` | Approve, reject, or request information. Approval queues Airwallex as `PENDING` |
| User | `POST /internal/inheritance-cases` | Open an inheritance case. The server stores a hash of the uploaded evidence |
| User | `POST /internal/inheritance-cases/{caseId}/reviews` | Two different reviewers. The second approval freezes the estate and posts a balanced transfer to the heir |
| Ledger | `POST /internal/ledger/freezes` | TCC Try |
| Ledger | `POST /internal/ledger/freezes/{id}/confirm` | TCC Confirm |
| Ledger | `POST /internal/ledger/freezes/{id}/cancel` | TCC Cancel |
| Ledger | `POST /internal/ledger/journals` | Submit a balanced journal |
| Ledger | `POST /internal/ops/dead-letters/{id}/replay` | Requeue one application dead letter |
| Risk | `POST /internal/risk/checks` | Synchronous gate |
| Valuation | `POST /internal/valuation/conversions` | Quote into a target currency |
| Notify | `POST /internal/notifications` | Submit one notification |
| Funding | `POST /internal/funding/virtual-ibans` | Issue a virtual IBAN |
| Funding | `POST /internal/funding/withdrawals` | Submit a withdrawal |
| Funding | `POST /internal/funding/fiat-settlements` | Record fiat settlement |
| Exchange | `POST /internal/exchange/locks` | Lock a price |
| Matching | `POST /internal/matching/orders` | Accept an order |
| Crypto | `POST /internal/crypto/deposit-addresses` | Allocate a deposit address |
| Crypto | `POST /internal/crypto/signatures` | Custody signature. Still 501 |
| Physical | `POST /internal/physical/intakes` | Record one physical intake |
| Trading | `POST /internal/trading/market-orders` | Open a market order |

When an inbound request has no `X-Trace-Id`, the service filter creates a UUID, stores it in the MDC, and writes it back on the response.

## Runtime dependencies

Any business-service profile other than `test` imports `classpath:digitalunionpay-nacos.yml` and the optional `nacos:<service-name>.yaml`. The gateway does not import that file.

| Variable | Default | Purpose |
| --- | --- | --- |
| `NACOS_ADDR` | `127.0.0.1:8848` | Registry and config server |
| `NACOS_USERNAME` | `nacos` | Nacos username |
| `NACOS_PASSWORD` | `nacos` | Nacos password |
| `SENTINEL_DASHBOARD` | `127.0.0.1:8080` | Sentinel dashboard. This port is also the Nacos console |
| `ROCKETMQ_NAMESRV` | `127.0.0.1:9876` | RocketMQ NameServer |
| `SEATA_ADDR` | `127.0.0.1:7091` | Seata transaction coordinator |
| `MYSQL_URL` | `jdbc:mysql://127.0.0.1:3306/digital_union_pay?...&serverTimezone=UTC` | Shared local database |
| `MYSQL_USERNAME` | `root` | Database user |
| `MYSQL_PASSWORD` | `root` | Database password |
| `DUP_AUTH_JWT_SECRET` | `DigitalUnionPayJwtSecretKey32B!!` | JWT HMAC secret, at least 32 bytes |
| `DUP_SERVICE_AUTH_SECRET` | `DigitalUnionPayServiceSecretKey32` | Service HMAC secret |
| `DUP_EDGE_SECRET` | `DigitalUnionPayEdgeSecretKey32!!` | Edge HMAC secret |
| `USER_SERVICE_URI` | `http://127.0.0.1:8081` | Login upstream used by the gateway |
| `OTLP_ENDPOINT` | unset | When set, traces are exported. Empty does not enable export |

The Seata transaction group is `digitalunionpay-tx-group`, registered in Nacos group `SEATA_GROUP`. The data-source proxy is off by default.

The `test` profile turns off Nacos, Sentinel, Seata, service HMAC, JWT enforcement, outbox publishing, and the RocketMQ health check. Tests use H2 in MySQL mode.

## Build and test

From the repository root, with JDK 21 or newer (this workspace uses `D:\jdk`):

```bash
mvn test
```

Build the gateway, user service, and ledger service:

```bash
mvn -pl dup-gateway,dup-user-service,dup-ledger-service -am test
```

Kubernetes manifests are in `deploy/k8s`. The images are not built. Secrets in that folder are placeholders.

## Current state

User registration, login, RBAC, JWT, service HMAC, ledger journals, balances, TCC fence, outbox, and the edge gateway are wired. Registration creates `rbac_user` and `user_profile` only. Funding, risk, valuation, notify, exchange, matching, crypto, physical, and trading ports are still skeletons and return `PORT_NOT_IMPLEMENTED` after input checks. Their new tables, and the fund-account tables, are reserved for the next implementation and are not written by those ports.

Later implementations should replace the skeletons and keep the current ports and paths.
