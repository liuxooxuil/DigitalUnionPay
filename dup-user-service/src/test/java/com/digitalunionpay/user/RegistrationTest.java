package com.digitalunionpay.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.support.TraceIdFilter;
import com.digitalunionpay.user.auth.Digests;
import com.digitalunionpay.user.ledger.LocalCustomerLedger;
import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 手机号或邮箱注册。成功后主账户待 KYC，账本余额为 0。 */
@SpringBootTest
@ActiveProfiles("test")
class RegistrationTest {

  @Autowired
  WebApplicationContext context;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  LocalCustomerLedger ledger;

  @Autowired
  TraceIdFilter traceIdFilter;

  @Autowired
  Filter springSecurityFilterChain;

  private MockMvc mvc;

  /** 装上链路号和 Spring Security。测试剖面关闭 token 校验。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter, springSecurityFilterChain).build();
  }

  /** 验证码通过后可以登录。档案停在待 KYC，主账户余额是 0。 */
  @Test
  void registerCreatesAMainAccountWithZeroBalance() throws Exception {
    String phone = "13800001001";
    String code = code("SMS", "86", phone, null, "REGISTER");
    String body = mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(sms(phone, code, "pass1234")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("+8613800001001"))
        .andExpect(jsonPath("$.userStatus").value("PENDING_KYC"))
        .andExpect(jsonPath("$.kycStatus").value("NOT_SUBMITTED"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    String userId = body.split("\"userId\":\"")[1].split("\"")[0];
    String accountNo = body.split("\"accountNo\":\"")[1].split("\"")[0];
    assertTrue(userId.startsWith("u"));
    assertTrue(accountNo.startsWith("MA"));
    assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM rbac_user WHERE user_id = ?", String.class, userId));
    assertTrue(jdbc.queryForObject("SELECT password_hash FROM rbac_user WHERE user_id = ?", String.class, userId).startsWith("$argon2id$"));
    assertEquals("PENDING_KYC", jdbc.queryForObject(
        "SELECT status FROM fund_account WHERE account_no = ?", String.class, accountNo));
    assertEquals(0, ledger.balances(userId).get(0).available());
    assertEquals(0, ledger.balances(userId).get(0).frozen());
    mvc.perform(post(AuthPaths.LOGIN)
            .header("X-Forwarded-For", "10.20.1.1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"+8613800001001\",\"password\":\"pass1234\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId));
  }

  /** 同一个手机号再注册一次返回 409。 */
  @Test
  void duplicatePhoneIsConflict() throws Exception {
    String phone = "13800001002";
    String first = code("SMS", "86", phone, null, "REGISTER");
    mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(sms(phone, first, "pass1234")))
        .andExpect(status().isOk());
    String second = code("SMS", "86", phone, null, "REGISTER");
    mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(sms(phone, second, "pass1234")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("phone"));
  }

  /** 邮箱只能注册一次，密码必须同时有字母和数字。 */
  @Test
  void emailRegistersOnceAndWeakPasswordIsRejected() throws Exception {
    mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.3")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"EMAIL\",\"email\":\"weak@example.com\",\"code\":\"000000\",\"password\":\"password\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password"));
    String code = code("EMAIL", null, null, "ada@example.com", "REGISTER");
    mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.3")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"EMAIL\",\"email\":\"ada@example.com\",\"code\":\"" + code + "\",\"password\":\"pass1234\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("ada@example.com"));
    String again = code("EMAIL", null, null, "ada@example.com", "REGISTER");
    mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.3")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"EMAIL\",\"email\":\"Ada@Example.com\",\"code\":\"" + again + "\",\"password\":\"pass1234\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("email"));
  }

  /** 同一 IP 一小时内第 6 次注册被拒绝。 */
  @Test
  void sixthRegistrationFromOneIpIsLimited() throws Exception {
    for (int i = 0; i < 5; i++) {
      jdbc.update(
          "INSERT INTO registration_event (id, client_ip, user_id, created_at) VALUES (?, '10.20.1.9', 'seed', CURRENT_TIMESTAMP)",
          900000000000L + i);
    }
    String code = code("SMS", "86", "13800001009", null, "REGISTER");
    mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", "10.20.1.9")
            .contentType(MediaType.APPLICATION_JSON)
            .content(sms("13800001009", code, "pass1234")))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("ip"));
  }

  /** 短信每天最多 10 次。响应正文里没有验证码。 */
  @Test
  void smsIsCappedAtTenPerDay() throws Exception {
    for (int i = 0; i < 10; i++) {
      code("SMS", "86", "13800001010", null, "REGISTER");
    }
    mvc.perform(post(AuthPaths.VERIFICATION_CODES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"SMS\",\"countryCode\":\"86\",\"phone\":\"13800001010\",\"purpose\":\"REGISTER\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("sms"));
  }

  /** 发出验证码并读出本地消息里的明文。接口响应不能带上这段明文。 */
  private String code(String channel, String country, String phone, String email, String purpose) throws Exception {
    String json = "SMS".equals(channel)
        ? "{\"channel\":\"SMS\",\"countryCode\":\"" + country + "\",\"phone\":\"" + phone + "\",\"purpose\":\"" + purpose + "\"}"
        : "{\"channel\":\"EMAIL\",\"email\":\"" + email + "\",\"purpose\":\"" + purpose + "\"}";
    String http = mvc.perform(post(AuthPaths.VERIFICATION_CODES).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    String target = "SMS".equals(channel) ? "+" + country + phone : email.toLowerCase();
    String hash = jdbc.queryForObject(
        """
        SELECT code_hash FROM verification_code
        WHERE channel = ? AND target = ? AND purpose = ? AND consumed_at IS NULL
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """,
        String.class,
        channel,
        target,
        purpose);
    List<String> payloads = jdbc.queryForList(
        "SELECT payload FROM dup_outbox WHERE event_type = 'user.verification' AND payload LIKE ?",
        String.class,
        channel + "|" + target + "|" + purpose + "|%");
    String value = null;
    for (String payload : payloads) {
      String candidate = payload.substring(payload.lastIndexOf('|') + 1);
      if (Digests.sha256(candidate).equals(hash)) {
        value = candidate;
        break;
      }
    }
    if (value == null) {
      throw new AssertionError("code");
    }
    assertFalse(http.contains(value));
    return value;
  }

  /** 手机号注册请求体。 */
  private static String sms(String phone, String code, String password) {
    return "{\"channel\":\"SMS\",\"countryCode\":\"86\",\"phone\":\"" + phone + "\",\"code\":\"" + code + "\",\"password\":\"" + password + "\"}";
  }
}
