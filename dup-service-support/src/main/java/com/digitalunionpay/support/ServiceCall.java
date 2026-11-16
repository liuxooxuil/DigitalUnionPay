package com.digitalunionpay.support;

import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpRequest;

/** 服务间 HMAC 签名。时间窗外或同一个 nonce 再出现都拒绝。 */
public final class ServiceCall {

  /** 允许的时钟偏差。 */
  public static final long SKEW_MILLIS = 300_000L;

  /** 调用时间，毫秒。 */
  public static final String TIME = "X-Service-Time";

  /** 一次调用的随机数。 */
  public static final String NONCE = "X-Service-Nonce";

  /** HMAC-SHA256 十六进制。 */
  public static final String SIGN = "X-Service-Sign";

  private final String secret;

  private final Clock clock;

  private final NonceStore nonces;

  /** 密钥、时钟和 nonce 表一起用。 */
  public ServiceCall(String secret, Clock clock, NonceStore nonces) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("secret");
    }
    if (nonces == null) {
      throw new IllegalArgumentException("nonces");
    }
    this.secret = secret;
    this.clock = clock == null ? Clock.systemUTC() : clock;
    this.nonces = nonces;
  }

  /** 给出站请求补上时间、nonce 和签名。 */
  public void write(HttpRequest request, byte[] body) {
    String timestamp = Long.toString(clock.millis());
    String nonce = java.util.UUID.randomUUID().toString();
    String path = request.getURI().getRawPath();
    if (path == null || path.isBlank()) {
      path = "/";
    }
    String sign = sign(request.getMethod().name(), path, timestamp, nonce, body);
    request.getHeaders().set(TIME, timestamp);
    request.getHeaders().set(NONCE, nonce);
    request.getHeaders().set(SIGN, sign);
  }

  /** 签名不对、时间过期或 nonce 重复时拒绝。 */
  public void verify(String method, String path, String timestamp, String nonce, byte[] body, String sign) {
    if (timestamp == null || nonce == null || nonce.isBlank() || sign == null || sign.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "signature");
    }
    long sent;
    try {
      sent = Long.parseLong(timestamp);
    } catch (NumberFormatException exception) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "signature");
    }
    if (Math.abs(clock.millis() - sent) > SKEW_MILLIS) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "timestamp");
    }
    String expected = sign(method, path, timestamp, nonce, body);
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sign.getBytes(StandardCharsets.UTF_8))) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "signature");
    }
    if (!nonces.remember(nonce)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED, "nonce");
    }
  }

  /** 方法、路径、时间、nonce 和报文体摘要拼在一起再做 HMAC。 */
  public String sign(String method, String path, String timestamp, String nonce, byte[] body) {
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
  private static String sha256(byte[] body) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(body == null ? new byte[0] : body);
      return HexFormat.of().formatHex(digest);
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("body");
    }
  }
}
