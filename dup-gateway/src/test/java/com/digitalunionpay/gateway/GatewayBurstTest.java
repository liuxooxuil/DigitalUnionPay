package com.digitalunionpay.gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** 同一用户在窗口内超过次数返回 429。限额只在这个测试上下文里收紧。 */
@SpringBootTest(properties = "dup.edge.permits=2")
@ActiveProfiles("test")
class GatewayBurstTest {

  private static final String EDGE = "DigitalUnionPayEdgeSecretKey32!!";

  private static final String JWT = "DigitalUnionPayJwtSecretKey32B!!";

  @Autowired
  WebApplicationContext context;

  @Autowired
  EdgeGate edgeGate;

  private MockMvc mvc;

  /** MockMvc 不会自动套上 Servlet Filter，这里显式装上边缘过滤器。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(edgeGate).build();
  }

  /** 第三次探测被频率限制拦住。 */
  @Test
  void thirdCallIsRateLimited() throws Exception {
    SecretKey key = new SecretKeySpec(JWT.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("digitalunionpay")
        .subject("user-burst")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(600))
        .claim("uid", "user-burst")
        .claim("grants", List.of("GET /api/edge/ping"))
        .build();
    String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    for (int i = 0; i < 2; i++) {
      String nonce = UUID.randomUUID().toString();
      String timestamp = Long.toString(System.currentTimeMillis());
      String sign = EdgeSigner.sign(EDGE, "GET", "/api/edge/ping", timestamp, nonce, new byte[0]);
      mvc.perform(get("/api/edge/ping")
              .header(EdgeSigner.TIME, timestamp)
              .header(EdgeSigner.NONCE, nonce)
              .header(EdgeSigner.SIGN, sign)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());
    }
    String nonce = UUID.randomUUID().toString();
    String timestamp = Long.toString(System.currentTimeMillis());
    String sign = EdgeSigner.sign(EDGE, "GET", "/api/edge/ping", timestamp, nonce, new byte[0]);
    mvc.perform(get("/api/edge/ping")
            .header(EdgeSigner.TIME, timestamp)
            .header(EdgeSigner.NONCE, nonce)
            .header(EdgeSigner.SIGN, sign)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
  }
}
