package com.digitalunionpay.crypto;

/** 加密资产服务的内部 HTTP 路径。 */
public final class CryptoPaths {
  /** 查询充值地址。 */
  public static final String DEPOSIT_ADDRESSES = "/internal/crypto/deposit-addresses";
  /** 提交托管签名。 */
  public static final String SIGNATURES = "/internal/crypto/signatures";

  /** 禁止实例化。 */
  private CryptoPaths() {
  }
}
