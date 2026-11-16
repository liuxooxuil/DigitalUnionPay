package com.digitalunionpay.gateway;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** 边缘进程自己的时钟和 JWT 解码器。不连接数据库，也不启用 Spring Security 过滤链。 */
@Configuration
class EdgeConfiguration {

  /** 签名和令牌都用 UTC。 */
  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }

  /** 与用户服务相同的 HS256 密钥。 */
  @Bean
  JwtDecoder jwtDecoder(@Value("${dup.auth.jwt-secret}") String secret) {
    return EdgeJwt.decoder(secret);
  }
}
