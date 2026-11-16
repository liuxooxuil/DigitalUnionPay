package com.digitalunionpay.gateway;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** 边缘验签用的 HS256。签发仍在用户服务，这里只认同一个密钥和签发方。 */
final class EdgeJwt {

  /** 与用户服务 AccessJwt 的 iss 一致。 */
  static final String ISSUER = "digitalunionpay";

  private EdgeJwt() {
  }

  /** 校验签名、过期时间和签发方。密钥至少 32 字节。 */
  static JwtDecoder decoder(String secret) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalArgumentException("jwt-secret");
    }
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
    return decoder;
  }
}
