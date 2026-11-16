package com.digitalunionpay.support;

/** 当前请求已经校验过的访问 token。出站 HTTP 从这里往下传。 */
public final class AccessContext {

  private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

  /** 禁止实例化。 */
  private AccessContext() {
  }

  /** 记下本线程的访问 token。 */
  public static void set(String token) {
    TOKEN.set(token);
  }

  /** 没有 token 时返回 null。 */
  public static String get() {
    return TOKEN.get();
  }

  /** 请求结束后清掉，避免线程复用时串到下一次调用。 */
  public static void clear() {
    TOKEN.remove();
  }
}
