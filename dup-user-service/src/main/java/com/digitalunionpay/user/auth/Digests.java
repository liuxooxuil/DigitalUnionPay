package com.digitalunionpay.user.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 十六进制。验证码和证件内容只存摘要。 */
public final class Digests {

  /** 禁止实例化。 */
  private Digests() {
  }

  /** 计算 UTF-8 文本的 SHA-256。 */
  public static String sha256(String value) {
    return sha256(value.getBytes(StandardCharsets.UTF_8));
  }

  /** 计算字节的 SHA-256。 */
  public static String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("sha256");
    }
  }
}
