package com.digitalunionpay.gateway;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 边缘 HMAC。规范串与服务间签名相同，密钥不是服务间密钥。 */
public final class EdgeSigner {

  /** 调用时间，毫秒。 */
  public static final String TIME = "X-Edge-Time";

  /** 一次调用的随机数。 */
  public static final String NONCE = "X-Edge-Nonce";

  /** HMAC-SHA256 十六进制。 */
  public static final String SIGN = "X-Edge-Sign";

  /** 允许的时钟偏差。 */
  public static final long SKEW_MILLIS = 300_000L;

  private EdgeSigner() {
  }

  /** 方法、路径、时间、nonce 和报文体摘要拼在一起再做 HMAC。 */
  public static String sign(String secret, String method, String path, String timestamp, String nonce, byte[] body) {
    String canonical = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + sha256(body);
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("signature");
    }
  }

  /** 空报文体按空字节摘要。 */
  static String sha256(byte[] body) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(body == null ? new byte[0] : body);
      return HexFormat.of().formatHex(digest);
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("body");
    }
  }
}
