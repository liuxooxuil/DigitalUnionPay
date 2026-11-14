package com.digitalunionpay.support;

/** Nacos 上的服务名，和各进程的 spring.application.name 一致。 */
public final class ServiceNames {

  public static final String USER = "user-service";
  public static final String LEDGER = "ledger-service";
  public static final String RISK = "risk-service";
  public static final String VALUATION = "valuation-service";
  public static final String NOTIFY = "notify-service";
  public static final String FUNDING = "funding-service";
  public static final String EXCHANGE = "exchange-service";
  public static final String MATCHING = "matching-service";
  public static final String CRYPTO = "crypto-service";
  public static final String PHYSICAL = "physical-service";
  public static final String TRADING = "trading-service";

  private ServiceNames() {
  }

  public static String http(String serviceName) {
    return "http://" + serviceName;
  }
}
