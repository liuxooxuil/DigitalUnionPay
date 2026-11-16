package com.digitalunionpay.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.support.ServiceCall;
import com.digitalunionpay.support.TraceIdFilter;
import jakarta.servlet.Filter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 打开服务签名后，缺签名、错误签名和重复 nonce 都要拒绝。 */
@SpringBootTest(properties = "dup.service-auth.enabled=true")
@ActiveProfiles("test")
class ServiceCallTest {

  @Autowired
  WebApplicationContext context;

  @Autowired
  TraceIdFilter traceIdFilter;

  @Autowired
  Filter springSecurityFilterChain;

  @Autowired
  ServiceCall calls;

  private MockMvc mvc;

  /** 装上链路号和安全过滤器。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter, springSecurityFilterChain).build();
  }

  /** 没有签名头时返回 401。 */
  @Test
  void missingSignatureIsUnauthorized() throws Exception {
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, "user-1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.message").value("signature"));
  }

  /** 签名正确时仍到达未实现的端口。 */
  @Test
  void signedCallReachesThePort() throws Exception {
    String path = "/internal/users/user-1/funding-allowed";
    String timestamp = Long.toString(System.currentTimeMillis());
    String nonce = UUID.randomUUID().toString();
    String sign = calls.sign("GET", path, timestamp, nonce, new byte[0]);
    mvc.perform(get(path)
            .header(ServiceCall.TIME, timestamp)
            .header(ServiceCall.NONCE, nonce)
            .header(ServiceCall.SIGN, sign))
        .andExpect(status().is(501));
  }

  /** 同一个 nonce 再用一次按重放拒绝。 */
  @Test
  void replayedNonceIsUnauthorized() throws Exception {
    String path = "/internal/users/user-1/funding-allowed";
    String timestamp = Long.toString(System.currentTimeMillis());
    String nonce = UUID.randomUUID().toString();
    String sign = calls.sign("GET", path, timestamp, nonce, new byte[0]);
    mvc.perform(get(path)
            .header(ServiceCall.TIME, timestamp)
            .header(ServiceCall.NONCE, nonce)
            .header(ServiceCall.SIGN, sign))
        .andExpect(status().is(501));
    mvc.perform(get(path)
            .header(ServiceCall.TIME, timestamp)
            .header(ServiceCall.NONCE, nonce)
            .header(ServiceCall.SIGN, sign))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("nonce"));
  }
}
