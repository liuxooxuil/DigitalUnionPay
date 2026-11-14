# DigitalUnionPay

数字资产与法币资金平台。当前版本 `0.1.0-SNAPSHOT` 是服务骨架：模块边界、内部接口和进程已经分开，业务端口的实现先校验入参，再返回 `PORT_NOT_IMPLEMENTED`。

客户余额只在清结算里变更。其它服务通过 HTTP 调用清结算，不直接改账。

## 技术栈

| 项 | 版本 |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.0.9 |
| Spring Cloud | 2025.1.4 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| 注册与配置 | Nacos |
| 限流 | Sentinel |
| 分布式事务 | Seata |
| 消息 | RocketMQ |

父工程是 `com.digitalunionpay:digital-union-pay`。每个业务域拆成两个模块：

- `*-api`：契约。记录类型、枚举、端口接口和 HTTP 客户端。
- `*-service`：独立 Spring Boot 进程。控制器把请求交给端口实现。

## 模块

| 模块 | 进程 | 端口 | 职责 |
| --- | --- | --- | --- |
| `dup-common` | — | — | 金额、错误码、标识校验。不放业务流程 |
| `dup-service-support` | — | — | 链路号、Nacos、Sentinel、Seata、RocketMQ、带追踪的 HTTP 客户端 |
| `dup-user-api` / `dup-user-service` | user-service | 8081 | 注册、会话、KYC、账户状态、继承。不改余额 |
| `dup-ledger-api` / `dup-ledger-service` | ledger-service | 8082 | 复式分录和 TCC。资金变更的唯一入口 |
| `dup-risk-api` / `dup-risk-service` | risk-service | 8083 | 入金、出金、兑换、交易前的限额与制裁拦截 |
| `dup-valuation-api` / `dup-valuation-service` | valuation-service | 8084 | 汇率、金价、加密资产报价。只读 |
| `dup-notify-api` / `dup-notify-service` | notify-service | 8085 | 站内信、短信、邮件、App Push |
| `dup-funding-api` / `dup-funding-service` | funding-service | 8086 | 法币出入金。虚拟 IBAN 与出金申请 |
| `dup-exchange-api` / `dup-exchange-service` | exchange-service | 8087 | 法币与稳定币兑换。锁价后双边记账 |
| `dup-matching-api` / `dup-matching-service` | matching-service | 8088 | 订单簿。成交后的资金交给清结算 |
| `dup-crypto-api` / `dup-crypto-service` | crypto-service | 8089 | 充值地址、确认数、托管签名 |
| `dup-physical-api` / `dup-physical-service` | physical-service | 8090 | 实物入金。设备读数和 RFID |
| `dup-trading-api` / `dup-trading-service` | trading-service | 8091 | 自营交易。MT5 网关，保证金和盈亏入账 |

服务名定义在 `com.digitalunionpay.support.ServiceNames`，与各进程的 `spring.application.name` 一致。服务间地址形如 `http://ledger-service`，由 Nacos 与负载均衡解析。

## 边界

- 清结算 `TccLedger` 是唯一的记账入口：`tryFreeze` 预冻结，`confirm` 入账，`cancel` 回滚，`post` 直接入账。
- `post` 只接受已经借贷平衡的分录。同一币种的借方最小单位合计必须等于贷方，且该币种在一笔分录里的 `scale` 必须相同。
- 法币到账入口是 `FundingCoordinator.onFiatSettled`。顺序是 KYC、风控，再进入清结算 TCC。兑换不在出入金模块里调用。
- 估值只提供报价，不改余额。
- 用户模块管 KYC 和继承案件。继承通过后，资金迁移由清结算完成。
- 加密资产的托管签名约束是 3-of-5 阈值签名，再加 HSM 二次确认。客户余额仍走清结算。
- 通知的业务侧只提交事件，不关心通道实现。进程间通知走 RocketMQ，topic 为 `digitalunionpay-domain-events`。

## 金额

`Money` 用最小单位的整数，避免浮点。

- `minorUnits` 必须大于或等于 0。
- `scale` 取值 0 到 18。
- `currency` 为 3 到 10 位大写字母或数字。

标识字段经过 `Ids.required`，空值和空白会在进入业务前被拒绝。

## 状态

| 领域 | 取值 |
| --- | --- |
| 用户 | `PENDING_KYC` → `ACTIVE` / `FROZEN` / `CLOSED` / `INHERITED` |
| KYC | `NOT_SUBMITTED` → `IN_REVIEW` → `APPROVED` / `REJECTED` / `RFI`。只有 `APPROVED` 允许出入金 |
| 账户 | `NORMAL` / `FROZEN` / `CLOSED` |
| 继承 | `PENDING` → `UNDER_REVIEW` → `FROZEN` → `COMPLETED` / `REJECTED` |
| 法币入金 | `PENDING_CONFIRM` → `RECEIVED` → `POSTED` / `EXCEPTION` / `REFUND` |
| 出金 | `APPLIED` → `IN_REVIEW` → `PROCESSING` → `SUCCEEDED` / `FAILED` |
| 分录方向 | `DEBIT` / `CREDIT` |
| 风控动作 | `DEPOSIT` / `WITHDRAW` / `EXCHANGE` / `TRADE` |
| 风控等级 | `LOW` / `MEDIUM` / `HIGH` |
| 通知事件 | `DEPOSIT_POSTED`、`EXCHANGE_RESULT`、`WITHDRAWAL_RESULT`、`RISK_BLOCKED`、`KYC_RESULT`、`INHERITANCE_PROGRESS`、`MARGIN_CALL` |
| 通知通道 | `INBOX` / `SMS` / `EMAIL` / `PUSH` |

错误码目前只有两个：`UNBALANCED_JOURNAL`、`PORT_NOT_IMPLEMENTED`。

## 内部接口

这些路径供服务之间调用，前缀都是 `/internal`。

| 服务 | 方法与路径 | 作用 |
| --- | --- | --- |
| 用户 | `GET /internal/users/{userId}/funding-allowed` | 出入金和兑换前查询 KYC 是否已通过 |
| 用户 | `POST /internal/inheritance-cases` | 打开继承案件 |
| 清结算 | `POST /internal/ledger/freezes` | TCC Try，按 reference 幂等预冻结 |
| 清结算 | `POST /internal/ledger/freezes/{id}/confirm` | TCC Confirm |
| 清结算 | `POST /internal/ledger/freezes/{id}/cancel` | TCC Cancel |
| 清结算 | `POST /internal/ledger/journals` | 提交已平衡的复式分录 |
| 风控 | `POST /internal/risk/checks` | 同步拦截 |
| 估值 | `POST /internal/valuation/conversions` | 按源金额和目标币种报价 |
| 通知 | `POST /internal/notifications` | 提交一条通知 |
| 出入金 | `POST /internal/funding/virtual-ibans` | 签发虚拟 IBAN |
| 出入金 | `POST /internal/funding/withdrawals` | 提交出金 |
| 出入金 | `POST /internal/funding/fiat-settlements` | 法币到账 |
| 兑换 | `POST /internal/exchange/locks` | 锁价 |
| 撮合 | `POST /internal/matching/orders` | 接受订单 |
| 加密资产 | `POST /internal/crypto/deposit-addresses` | 分配充值地址 |
| 加密资产 | `POST /internal/crypto/signatures` | 托管签名 |
| 实物 | `POST /internal/physical/intakes` | 记录一次实物入金 |
| 交易 | `POST /internal/trading/market-orders` | 开市价单 |

入站请求没有 `X-Trace-Id` 时，过滤器会生成一个 UUID，写入 MDC 和响应头。出站 HTTP 调用沿用同一条链路号。日志级别模式是 `%5p [trace:%X{X-Trace-Id:-}]`。

健康检查暴露在 `/actuator/health`。

## 运行依赖

非 `test` profile 会导入 `classpath:digitalunionpay-nacos.yml`，以及可选的 `nacos:<服务名>.yaml`。

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `NACOS_ADDR` | `127.0.0.1:8848` | 注册中心和配置中心 |
| `NACOS_USERNAME` | `nacos` | Nacos 用户名 |
| `NACOS_PASSWORD` | `nacos` | Nacos 密码 |
| `SENTINEL_DASHBOARD` | `127.0.0.1:8080` | Sentinel 控制台 |
| `ROCKETMQ_NAMESRV` | `127.0.0.1:9876` | RocketMQ NameServer |
| `SEATA_ADDR` | `127.0.0.1:7091` | Seata TC |

Seata 事务组是 `digitalunionpay-tx-group`，注册到 Nacos 的 `SEATA_GROUP`。数据源代理默认关闭（`enable-auto-data-source-proxy: false`）。

`test` profile 关闭 Nacos、Sentinel、Seata 和 RocketMQ 健康检查，供上下文测试使用。

## 构建与测试

在仓库根目录执行：

```bash
mvn test
```

只构建一个服务及其依赖：

```bash
mvn -pl dup-ledger-service -am test
```

启动一个进程（需要上表中的基础设施，或改用已准备好的 Nacos 配置）：

```bash
mvn -pl dup-ledger-service spring-boot:run
```

各服务测试目前验证 Spring 上下文可以在 `test` profile 下启动。业务端口尚未接入真实渠道、账本存储或外部行情。

## 现状

每个服务的 `internal` 包里是 `Skeleton*` 实现。它们检查空入参和尚未注入的协作者，然后抛出 `BusinessException(PORT_NOT_IMPLEMENTED)`。清结算的 `post` 会先调用 `JournalCommand.requireBalanced()`，不平衡的分录在未实现存储之前就会被拒绝。

后续实现应替换这些骨架，保持现有端口和路径不变。
