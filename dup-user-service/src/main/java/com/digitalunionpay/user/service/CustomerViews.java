package com.digitalunionpay.user.service;

/** 用户模块的响应体。成功时不加 ApiResult 外壳。 */
public final class CustomerViews {

  /** 禁止实例化。 */
  private CustomerViews() {
  }

  /** 生物识别挑战。两分钟内有效，只用一次。 */
  public record Challenge(String challenge) {
  }

  /** 一台仍在线的设备。current 表示这次请求用的会话。 */
  public record SessionDevice(
      long id,
      String deviceId,
      String deviceName,
      String clientIp,
      String locationKey,
      String lastSeenAt,
      boolean current) {
  }

  /** 证件一面已经收下。sha256 是内容摘要，提交 KYC 时要带回。 */
  public record DocumentReceipt(String side, String sha256) {
  }

  /** OCR 结果。recognized 为 false 时其余字段为空，不表示证件通过。 */
  public record OcrResult(
      boolean recognized,
      String fullName,
      String birthDate,
      String nationality,
      String address,
      String idType,
      String idNumber) {
  }

  /** 一次活体会话。证明是 sessionId、分数和密钥的 HMAC，不是客户端自报的分数。 */
  public record LivenessTicket(String sessionId, String expiresAt) {
  }

  /** 最新一笔 KYC。没有案件时 status 为 NOT_SUBMITTED。 */
  public record KycView(
      String status,
      String fullName,
      String birthDate,
      String nationality,
      String address,
      String idType,
      String idNumber,
      String rejectReason,
      String rfiDueAt) {
  }

  /** 当前用户档案和账户状态。 */
  public record AccountSnapshot(String userId, String userStatus, String kycStatus, String accountStatus, String kycLevel) {
  }

  /** 一个子账户在某个币种上的余额。fundKind 是 FIAT 或 CRYPTO。 */
  public record Holding(
      String accountNo,
      String fundCode,
      String fundKind,
      String currency,
      long available,
      long frozen,
      int scale) {
  }

  /** 已绑定的收款账户。账号只留后四位。 */
  public record BankAccountView(long id, String holderName, String bankName, String last4) {
  }

  /** 账户关闭申请。 */
  public record ClosureView(long id, String status) {
  }

  /** 继承案件当前状态。 */
  public record InheritanceView(String caseId, String status, String estateUserId, String heirUserId) {
  }

  /** 限额检查通过时的额度。超出时接口直接拒绝，不返回这个对象。 */
  public record LimitView(long singleMinor, long dailyMinor, long monthlyMinor, long usedDaily, long usedMonthly) {
  }
}
