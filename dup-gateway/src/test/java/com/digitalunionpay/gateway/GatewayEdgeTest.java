package com.digitalunionpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** 边缘隔离、签名、权限和登录降级。下游用户服务没有启动。 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayEdgeTest {

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

  /** 内部路径不能从边缘进来。 */
  @Test
  void internalPathIsRejected() throws Exception {
    mvc.perform(get("/internal/ledger/journals"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("internal"));
  }

  /** 外部不能直接访问资金前缀。 */
  @Test
  void fundingPrefixIsRejected() throws Exception {
    mvc.perform(post("/api/funding/withdrawals"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("external"));
  }

  /** 没有签名的探测被拒绝。 */
  @Test
  void missingSignatureIsRejected() throws Exception {
    mvc.perform(get("/api/edge/ping"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("signature"));
  }

  /** 同一 nonce 第二次使用被拒绝。 */
  @Test
  void replayedNonceIsRejected() throws Exception {
    String token = token("user-replay", "GET /api/edge/ping");
    MockHttpServletRequestBuilder first = signed(get("/api/edge/ping"), "GET", "/api/edge/ping", new byte[0], "nonce-replay");
    mvc.perform(first.header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    MockHttpServletRequestBuilder second = signed(get("/api/edge/ping"), "GET", "/api/edge/ping", new byte[0], "nonce-replay");
    mvc.perform(second.header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("nonce"));
  }

  /** 签名通过但没有令牌。 */
  @Test
  void missingTokenIsRejected() throws Exception {
    mvc.perform(signed(get("/api/edge/ping"), "GET", "/api/edge/ping", new byte[0], "nonce-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("token"));
  }

  /** 令牌没有这条路径的权限。 */
  @Test
  void missingGrantIsRejected() throws Exception {
    String token = token("user-grant", "GET /api/other");
    mvc.perform(signed(get("/api/edge/ping"), "GET", "/api/edge/ping", new byte[0], "nonce-grant")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("path"));
  }

  /** 签名和权限都通过时，探测在网关本地返回。 */
  @Test
  void signedPingIsUp() throws Exception {
    String token = token("user-ping", "GET /api/edge/ping");
    mvc.perform(signed(get("/api/edge/ping"), "GET", "/api/edge/ping", new byte[0], "nonce-ping")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("up"));
  }

  /** 被拒绝的来源地址不能登录。 */
  @Test
  void deniedIpIsRejected() throws Exception {
    mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.8").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("external"));
  }

  /** 登录不要求边缘签名。用户服务没启动时返回不可用，而不是登录成功。 */
  @Test
  void loginFallsBackWhenUserServiceIsDown() throws Exception {
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"a\",\"password\":\"b\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("UNAVAILABLE"));
  }

  /** 健康检查不要求边缘签名。 */
  @Test
  void actuatorDoesNotNeedSignature() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  /** 过期时间戳被拒绝。 */
  @Test
  void staleTimestampIsRejected() throws Exception {
    byte[] body = new byte[0];
    String sign = EdgeSigner.sign(EDGE, "GET", "/api/edge/ping", "1", "nonce-stale", body);
    mvc.perform(get("/api/edge/ping").header(EdgeSigner.TIME, "1").header(EdgeSigner.NONCE, "nonce-stale").header(EdgeSigner.SIGN, sign))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("timestamp"));
  }

  /** 不同 nonce 的签名不同。 */
  @Test
  void signatureChangesWithNonce() {
    String one = EdgeSigner.sign(EDGE, "POST", "/internal/auth/login", "1", "n1", new byte[0]);
    String two = EdgeSigner.sign(EDGE, "POST", "/internal/auth/login", "1", "n2", new byte[0]);
    assertNotEquals(one, two);
    assertEquals(64, one.length());
  }

  private static MockHttpServletRequestBuilder signed(
      MockHttpServletRequestBuilder builder, String method, String path, byte[] body, String nonce) {
    String timestamp = Long.toString(System.currentTimeMillis());
    String sign = EdgeSigner.sign(EDGE, method, path, timestamp, nonce, body);
    return builder.header(EdgeSigner.TIME, timestamp).header(EdgeSigner.NONCE, nonce).header(EdgeSigner.SIGN, sign);
  }

  private static String token(String subject, String grant) {
    SecretKey key = new SecretKeySpec(JWT.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("digitalunionpay")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(600))
        .claim("uid", subject)
        .claim("grants", List.of(grant))
        .build();
    return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
  }
}
