package com.digitalunionpay.support;

/** Nacos 上的服务名，和各进程的 spring.application.name 一致。 */
public final class ServiceNames {

  /** 用户服务。注册、KYC 和继承。 */
  public static final String USER = "user-service";
  /** 清结算服务。复式分录和 TCC。 */
  public static final String LEDGER = "ledger-service";
  /** 风控服务。入账前的限额和制裁拦截。 */
  public static final String RISK = "risk-service";
  /** 估值服务。汇率、金价和加密资产报价。 */
  public static final String VALUATION = "valuation-service";
  /** 通知服务。站内信、短信、邮件和 App Push。 */
  public static final String NOTIFY = "notify-service";
  /** 出入金服务。法币渠道和出金申请。 */
  public static final String FUNDING = "funding-service";
  /** 兑换服务。法币与稳定币锁价兑换。 */
  public static final String EXCHANGE = "exchange-service";
  /** 撮合服务。订单簿。 */
  public static final String MATCHING = "matching-service";
  /** 加密资产服务。充值地址和托管签名。 */
  public static final String CRYPTO = "crypto-service";
  /** 实物入金服务。设备读数和 RFID。 */
  public static final String PHYSICAL = "physical-service";
  /** 自营交易服务。MT5 网关。 */
  public static final String TRADING = "trading-service";

  /** 禁止实例化。 */
  private ServiceNames() {
  }

  /** 拼出供负载均衡解析的服务地址，形如 http://ledger-service。 */
  public static String http(String serviceName) {
    return "http://" + serviceName;
  }
}
