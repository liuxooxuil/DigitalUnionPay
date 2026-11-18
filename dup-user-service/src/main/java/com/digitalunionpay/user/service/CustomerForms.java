package com.digitalunionpay.user.service;

/** 用户模块的请求体。字段在服务里校验，这里只做承载。 */
public final class CustomerForms {

  /** 禁止实例化。 */
  private CustomerForms() {
  }

  /** 发送验证码。 */
  public record SendCode(String channel, String countryCode, String phone, String email, String purpose) {
  }

  /** 验证码登录。 */
  public record CodeLogin(
      String channel, String countryCode, String phone, String email, String code, String deviceId, String deviceName) {
  }

  /** 绑定生物识别公钥。公钥是 X.509 编码的 Ed25519，Base64。 */
  public record BindBiometric(String deviceId, String publicKey) {
  }

  /** 索取一次性挑战。 */
  public record BiometricChallenge(String deviceId) {
  }

  /** 用挑战的签名登录。 */
  public record BiometricLogin(String deviceId, String challenge, String signature) {
  }

  /** 上传证件的一面。contentBase64 是文件内容。 */
  public record DocumentUpload(String side, String contentType, String contentBase64) {
  }

  /** 用户确认后的 KYC 提交。活体证明是服务端密钥的 HMAC。 */
  public record KycSubmit(
      String fullName,
      String birthDate,
      String nationality,
      String address,
      String idType,
      String idNumber,
      String frontSha256,
      String backSha256,
      String livenessSessionId,
      int livenessScore,
      int faceScore,
      String livenessProof) {
  }

  /** 审核结论。status 为 APPROVED、REJECTED 或 RFI。 */
  public record KycDecision(String status, String reason) {
  }

  /** 补充材料沿用提交结构。 */
  public record PasswordChange(String oldPassword, String newPassword) {
  }

  /** 绑定新的手机号或邮箱，要先拿到 BIND 验证码。 */
  public record BindContact(String channel, String countryCode, String phone, String email, String code) {
  }

  /** 打开或关闭二次验证。 */
  public record MfaSwitch(boolean enabled) {
  }

  /** 绑定同名收款账户。 */
  public record BindBank(String holderName, String accountNumber, String bankName) {
  }

  /** 限额检查。minorUnits 是最小货币单位。 */
  public record LimitCheck(long minorUnits, int scale, String currency) {
  }

  /** 继承立案。evidenceBase64 由服务端计算哈希。 */
  public record OpenInheritance(String caseId, String estateUserId, String heirUserId, String applicantRole, String evidenceBase64) {
  }

  /** 复核。approve 为 false 时驳回。 */
  public record InheritanceReview(String reviewerId, boolean approve, String reason) {
  }

  /** 人工审核账户关闭申请。 */
  public record ClosureDecision(boolean approve, String reason) {
  }

  /** 把证件内容交给 OCR。无法识别时 recognized 为 false。 */
  public record OcrRequest(String contentBase64) {
  }
}
