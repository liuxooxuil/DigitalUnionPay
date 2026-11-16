package com.digitalunionpay.support;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** 各进程共用的 HS256 密钥。登录在用户服务签发，其他进程只验签。 */
public final class AccessJwt {

  /** JWT 的 iss。 */
  public static final String ISSUER = "digitalunionpay";

  /** 禁止实例化。 */
  private AccessJwt() {
  }

  /** 密钥至少 32 字节，否则 HS256 不能用。 */
  public static JwtEncoder encoder(String secret) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
  }

  /** 校验签名、过期时间和签发方。 */
  public static JwtDecoder decoder(String secret) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
    return decoder;
  }

  /** 把配置里的密钥变成 HMAC 密钥。 */
  private static SecretKey key(String secret) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalArgumentException("jwt-secret");
    }
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
