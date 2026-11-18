package com.digitalunionpay.user;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.support.TraceIdFilter;
import com.digitalunionpay.user.auth.Digests;
import com.digitalunionpay.user.auth.PasswordHashes;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.ledger.LocalCustomerLedger;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

/** 登录、KYC、账户和继承。验证码只从本地消息里取。 */
@SpringBootTest
@ActiveProfiles("test")
class CustomerModuleTest {

  private static final String LIVENESS = "DigitalUnionPayLivenessKey!!!!";

  @Autowired
  WebApplicationContext context;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  LocalCustomerLedger ledger;

  @Autowired
  RbacUserMapper users;

  @Autowired
  TraceIdFilter traceIdFilter;

  @Autowired
  Filter springSecurityFilterChain;

  private MockMvc mvc;

  /** 装上链路号和安全过滤器。 */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter, springSecurityFilterChain).build();
  }

  /** 连续 5 次密码错误后锁定。验证码登录不受这把锁影响。 */
  @Test
  void passwordLocksAfterFiveFailuresAndCodeLoginDoesNot() throws Exception {
    RbacUserEntity user = new RbacUserEntity();
    user.setUserId("user-lock");
    user.setUsername("lock@example.com");
    user.setEmail("lock@example.com");
    user.setPasswordHash(PasswordHashes.hash("pass1234"));
    user.setStatus("ACTIVE");
    users.insert(user);
    for (int i = 0; i < 4; i++) {
      mvc.perform(post(AuthPaths.LOGIN)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\":\"lock@example.com\",\"password\":\"wrong123\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("login"));
    }
    mvc.perform(post(AuthPaths.LOGIN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"lock@example.com\",\"password\":\"wrong123\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("locked"));
    String code = code("EMAIL", null, null, "lock@example.com", "LOGIN");
    mvc.perform(post(AuthPaths.LOGIN_CODE)
            .header("X-Forwarded-For", "10.40.1.1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"EMAIL\",\"email\":\"lock@example.com\",\"code\":\"" + code + "\",\"deviceId\":\"lock-device\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("user-lock"));
  }

  /** 不存在的手机号也返回成功，但不保存验证码。二次验证先要验证码。 */
  @Test
  void missingUserDoesNotStoreALoginCodeAndMfaRequiresOne() throws Exception {
    int before = jdbc.queryForObject(
        "SELECT COUNT(*) FROM verification_code WHERE target = '+860000000000'", Integer.class);
    mvc.perform(post(AuthPaths.VERIFICATION_CODES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"SMS\",\"countryCode\":\"86\",\"phone\":\"0000000000\",\"purpose\":\"LOGIN\"}"))
        .andExpect(status().isOk());
    int after = jdbc.queryForObject(
        "SELECT COUNT(*) FROM verification_code WHERE target = '+860000000000'", Integer.class);
    assertEquals(before, after);
    Registered registered = register("13800003001", "10.40.2.1");
    jdbc.update("UPDATE rbac_user SET mfa_enabled = 1 WHERE user_id = ?", registered.userId);
    mvc.perform(post(AuthPaths.LOGIN)
            .header("X-Forwarded-For", "10.40.2.1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + registered.username + "\",\"password\":\"pass1234\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("mfa"));
    String code = code("SMS", "86", "13800003001", null, "LOGIN");
    mvc.perform(post(AuthPaths.LOGIN)
            .header("X-Forwarded-For", "10.40.2.2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + registered.username + "\",\"password\":\"pass1234\",\"code\":\"" + code + "\",\"deviceId\":\"mfa-device\"}"))
        .andExpect(status().isOk());
  }

  /** 最多 5 台设备。同一台可以再次登录。空闲超时和远程下线都会让旧 token 失效。异地登录写告警。 */
  @Test
  void devicesAreCappedAndCanBeRevoked() throws Exception {
    Registered registered = register("13800003002", "10.40.3.1");
    String[] tokens = new String[5];
    for (int i = 0; i < 5; i++) {
      tokens[i] = login(registered.username, "10.1.3." + (i + 1), "device-" + i);
    }
    mvc.perform(post(AuthPaths.LOGIN)
            .header("X-Forwarded-For", "10.1.3.9")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + registered.username + "\",\"password\":\"pass1234\",\"deviceId\":\"device-6\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("device"));
    String again = login(registered.username, "10.9.3.1", "device-0");
    Integer alerts = jdbc.queryForObject(
        "SELECT COUNT(*) FROM dup_outbox WHERE event_type = 'user.login.alert' AND payload LIKE ?",
        Integer.class,
        registered.userId + "|REMOTE|device-0|%");
    assertEquals(1, alerts);
    mvc.perform(get(UserPaths.SESSIONS).header("Authorization", "Bearer " + again))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].deviceId", hasItem("device-0")));
    String other = jdbc.queryForObject(
        "SELECT id FROM auth_token WHERE user_id = ? AND device_id = 'device-1' AND revoked_at IS NULL",
        String.class,
        registered.userId);
    mvc.perform(delete(UserPaths.SESSION, other).header("Authorization", "Bearer " + again))
        .andExpect(status().isOk());
    mvc.perform(get(UserPaths.ME).header("Authorization", "Bearer " + tokens[1]))
        .andExpect(status().isUnauthorized());
    jdbc.update("UPDATE auth_token SET last_seen_at = '2000-01-01 00:00:00' WHERE user_id = ?", registered.userId);
    mvc.perform(get(UserPaths.SESSIONS).header("Authorization", "Bearer " + again))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("idle"));
  }

  /** 先绑定公钥，再用挑战签名登录。 */
  @Test
  void biometricLoginRequiresABoundKey() throws Exception {
    Registered registered = register("13800003003", "10.40.4.1");
    String token = login(registered.username, "10.40.4.1", "bio-device");
    KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
    KeyPair pair = generator.generateKeyPair();
    String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    mvc.perform(post(AuthPaths.SECURITY_DEVICES)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"bio-device\",\"publicKey\":\"" + publicKey + "\"}"))
        .andExpect(status().isOk());
    String challenge = mvc.perform(post(AuthPaths.BIOMETRIC_CHALLENGES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"bio-device\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .split("\"challenge\":\"")[1]
        .split("\"")[0];
    Signature signer = Signature.getInstance("Ed25519");
    signer.initSign(pair.getPrivate());
    signer.update(challenge.getBytes(StandardCharsets.UTF_8));
    String signature = Base64.getEncoder().encodeToString(signer.sign());
    mvc.perform(post(AuthPaths.LOGIN_BIOMETRIC)
            .header("X-Forwarded-For", "10.40.4.2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"bio-device\",\"challenge\":\"" + challenge + "\",\"signature\":\"" + signature + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(registered.userId));
  }

  /** 可以改密码、绑定邮箱，但不能解绑作为登录名的手机号。 */
  @Test
  void passwordAndEmailCanChange() throws Exception {
    Registered registered = register("13800003011", "10.40.9.1");
    String token = login(registered.username, "10.40.9.1", "sec-device");
    mvc.perform(post(AuthPaths.SECURITY_PASSWORD)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"oldPassword\":\"pass1234\",\"newPassword\":\"pass5678\"}"))
        .andExpect(status().isOk());
    mvc.perform(post(AuthPaths.LOGIN)
            .header("X-Forwarded-For", "10.40.9.1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + registered.username + "\",\"password\":\"pass5678\",\"deviceId\":\"sec-device-2\"}"))
        .andExpect(status().isOk());
    mvc.perform(post(AuthPaths.VERIFICATION_CODES)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"EMAIL\",\"email\":\"sec@example.com\",\"purpose\":\"BIND\"}"))
        .andExpect(status().isOk());
    String code = jdbc.queryForObject(
        "SELECT payload FROM dup_outbox WHERE event_type = 'user.verification' AND payload LIKE ? ORDER BY created_at DESC LIMIT 1",
        String.class,
        "EMAIL|sec@example.com|BIND|%");
    code = code.substring(code.lastIndexOf('|') + 1);
    mvc.perform(post(AuthPaths.SECURITY_CONTACTS)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"EMAIL\",\"email\":\"sec@example.com\",\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk());
    mvc.perform(delete(AuthPaths.SECURITY_CONTACTS)
            .header("Authorization", "Bearer " + token)
            .param("channel", "SMS"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("contact"));
    mvc.perform(delete(AuthPaths.SECURITY_CONTACTS)
            .header("Authorization", "Bearer " + token)
            .param("channel", "EMAIL"))
        .andExpect(status().isOk());
    mvc.perform(post(AuthPaths.SECURITY_MFA)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"enabled\":true}"))
        .andExpect(status().isOk());
    assertEquals(1, jdbc.queryForObject("SELECT mfa_enabled FROM rbac_user WHERE user_id = ?", Integer.class, registered.userId));
  }

  /** 通过 KYC 才能入金。限额、同名账户和零余额关闭都按档案走。 */
  @Test
  void kycApprovalOpensLimitsBankAndClosure() throws Exception {
    Registered registered = register("13800003004", "10.40.5.1");
    String token = login(registered.username, "10.40.5.1", "kyc-device");
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, registered.userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));
    approve(token, registered.userId, "Ada Lovelace");
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, registered.userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));
    mvc.perform(get(UserPaths.ME_BALANCES).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fundKind").value("FIAT"))
        .andExpect(jsonPath("$[0].available").value(0));
    mvc.perform(post(UserPaths.ME_LIMITS)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"minorUnits\":1,\"scale\":2,\"currency\":\"CNY\"}"))
        .andExpect(status().isOk());
    mvc.perform(post(UserPaths.ME_LIMITS)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"minorUnits\":5000001,\"scale\":2,\"currency\":\"CNY\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("limit"));
    mvc.perform(post(UserPaths.ME_BANK)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"holderName\":\"Other Name\",\"accountNumber\":\"6222000012345678\",\"bankName\":\"Test Bank\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("name"));
    mvc.perform(post(UserPaths.ME_BANK)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"holderName\":\"Ada Lovelace\",\"accountNumber\":\"6222000012345678\",\"bankName\":\"Test Bank\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.last4").value("5678"));
    ledger.setAvailable(registered.userId, registered.accountNo, "CNY", 2, 10);
    mvc.perform(post(UserPaths.ME_CLOSURE).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("balance"));
    ledger.setAvailable(registered.userId, registered.accountNo, "CNY", 2, 0);
    mvc.perform(post(UserPaths.ME_CLOSURE).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));
    mvc.perform(post(UserPaths.CLOSURE_DECISION, registered.userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"approve\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    assertEquals("CLOSED", jdbc.queryForObject(
        "SELECT account_status FROM user_profile WHERE user_id = ?", String.class, registered.userId));
    mvc.perform(get(UserPaths.ME).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  /** 制裁名单命中不进入审核。补充材料过期后变成拒绝。拒绝后可以重新提交。 */
  @Test
  void sanctionsBlockReviewAndRfiExpires() throws Exception {
    jdbc.update(
        "INSERT INTO sanctions_entry (id, name_key, source) SELECT 88001, 'JOHNDOE', 'OFAC' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sanctions_entry WHERE name_key = 'JOHNDOE' AND source = 'OFAC')");
    Registered blocked = register("13800003005", "10.40.6.1");
    String blockedToken = login(blocked.username, "10.40.6.1", "sanc-device");
    mvc.perform(post(UserPaths.ME_KYC)
            .header("Authorization", "Bearer " + blockedToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(kycJson(blockedToken, blocked.userId, "John Doe")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("sanctions"));
    assertEquals("NOT_SUBMITTED", jdbc.queryForObject(
        "SELECT kyc_status FROM user_profile WHERE user_id = ?", String.class, blocked.userId));

    Registered held = register("13800003006", "10.40.6.2");
    String heldToken = login(held.username, "10.40.6.2", "rfi-device");
    mvc.perform(post(UserPaths.ME_KYC)
            .header("Authorization", "Bearer " + heldToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(kycJson(heldToken, held.userId, "Grace Hopper")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    mvc.perform(post(UserPaths.KYC_DECISION, held.userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"RFI\",\"reason\":\"need address\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RFI"));
    jdbc.update("UPDATE kyc_case SET rfi_due_at = '2000-01-01 00:00:00' WHERE user_id = ?", held.userId);
    mvc.perform(post(UserPaths.ME_KYC)
            .header("Authorization", "Bearer " + heldToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(kycJson(heldToken, held.userId, "Grace Hopper")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("rfi-expired"));
    assertEquals("REJECTED", jdbc.queryForObject(
        "SELECT kyc_status FROM user_profile WHERE user_id = ?", String.class, held.userId));
  }

  /** 两个不同的复核人通过后，可用余额迁到已完成 KYC 的继承人，原账户标记已继承。 */
  @Test
  void inheritanceMovesFundsAfterTwoReviews() throws Exception {
    Registered estate = register("13800003007", "10.40.7.1");
    Registered heir = register("13800003008", "10.40.7.2");
    String estateToken = login(estate.username, "10.40.7.1", "estate-device");
    String heirToken = login(heir.username, "10.40.7.2", "heir-device");
    approve(estateToken, estate.userId, "Estate Person");
    approve(heirToken, heir.userId, "Heir Person");
    ledger.setAvailable(estate.userId, estate.accountNo, "CNY", 2, 100);
    byte[] evidence = "death-certificate".getBytes(StandardCharsets.UTF_8);
    String encoded = Base64.getEncoder().encodeToString(evidence);
    mvc.perform(post(UserPaths.INHERITANCE_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"caseId\":\"case-1\",\"estateUserId\":\"" + estate.userId + "\",\"heirUserId\":\"" + heir.userId
                + "\",\"applicantRole\":\"HEIR\",\"evidenceBase64\":\"" + encoded + "\"}"))
        .andExpect(status().isOk());
    assertEquals(Digests.sha256(evidence), jdbc.queryForObject(
        "SELECT evidence_sha256 FROM inheritance_case WHERE case_id = 'case-1'", String.class));
    mvc.perform(post(UserPaths.INHERITANCE_REVIEW, "case-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewerId\":\"compliance\",\"approve\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    mvc.perform(post(UserPaths.INHERITANCE_REVIEW, "case-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewerId\":\"compliance\",\"approve\":true}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("reviewer"));
    mvc.perform(post(UserPaths.INHERITANCE_REVIEW, "case-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewerId\":\"senior\",\"approve\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    assertEquals(0, ledger.balances(estate.userId).get(0).available());
    assertEquals(100, ledger.balances(heir.userId).get(0).available());
    assertEquals("INHERITED", jdbc.queryForObject(
        "SELECT user_status FROM user_profile WHERE user_id = ?", String.class, estate.userId));
    assertEquals("FROZEN", jdbc.queryForObject(
        "SELECT account_status FROM user_profile WHERE user_id = ?", String.class, estate.userId));
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, estate.userId))
        .andExpect(jsonPath("$").value(false));
  }

  /** 继承人还没通过 KYC 时，复审不能迁移资金。 */
  @Test
  void inheritanceWaitsForHeirKyc() throws Exception {
    Registered estate = register("13800003009", "10.40.8.1");
    Registered heir = register("13800003010", "10.40.8.2");
    String token = login(estate.username, "10.40.8.1", "wait-device");
    approve(token, estate.userId, "Waiting Estate");
    String encoded = Base64.getEncoder().encodeToString("will".getBytes(StandardCharsets.UTF_8));
    mvc.perform(post(UserPaths.INHERITANCE_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"caseId\":\"case-2\",\"estateUserId\":\"" + estate.userId + "\",\"heirUserId\":\"" + heir.userId
                + "\",\"applicantRole\":\"AGENT\",\"evidenceBase64\":\"" + encoded + "\"}"))
        .andExpect(status().isOk());
    mvc.perform(post(UserPaths.INHERITANCE_REVIEW, "case-2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewerId\":\"compliance-2\",\"approve\":true}"))
        .andExpect(status().isOk());
    mvc.perform(post(UserPaths.INHERITANCE_REVIEW, "case-2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewerId\":\"senior-2\",\"approve\":true}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("kyc"));
    assertEquals("UNDER_REVIEW", jdbc.queryForObject(
        "SELECT status FROM inheritance_case WHERE case_id = 'case-2'", String.class));
  }

  /** 注册一个手机号用户。 */
  private Registered register(String phone, String ip) throws Exception {
    String code = code("SMS", "86", phone, null, "REGISTER");
    String body = mvc.perform(post(AuthPaths.REGISTER)
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channel\":\"SMS\",\"countryCode\":\"86\",\"phone\":\"" + phone + "\",\"code\":\"" + code + "\",\"password\":\"pass1234\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return new Registered(
        body.split("\"userId\":\"")[1].split("\"")[0],
        "+86" + phone,
        body.split("\"accountNo\":\"")[1].split("\"")[0]);
  }

  /** 密码登录并取出 token。 */
  private String login(String username, String ip, String device) throws Exception {
    return mvc.perform(post(AuthPaths.LOGIN)
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass1234\",\"deviceId\":\"" + device + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .split("\"token\":\"")[1]
        .split("\"")[0];
  }

  /** 上传证件、活体通过并审核通过。OCR1 文本能识别，普通内容不能。 */
  private void approve(String token, String userId, String name) throws Exception {
    String text = "OCR1\nname: " + name + "\nbirthDate: 1990-01-02\nnationality: CN\naddress: Road 1\nidType: ID_CARD\nidNumber: 110101199001021234\n";
    String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    mvc.perform(post(UserPaths.ME_KYC_OCR)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"contentBase64\":\"" + encoded + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recognized").value(true))
        .andExpect(jsonPath("$.fullName").value(name));
    String junk = Base64.getEncoder().encodeToString("not-a-document".getBytes(StandardCharsets.UTF_8));
    mvc.perform(post(UserPaths.ME_KYC_OCR)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"contentBase64\":\"" + junk + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recognized").value(false));
    mvc.perform(post(UserPaths.ME_KYC)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(kycJson(token, userId, name)))
        .andExpect(status().isOk());
    mvc.perform(post(UserPaths.KYC_DECISION, userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    Integer pending = jdbc.queryForObject(
        "SELECT COUNT(*) FROM airwallex_account WHERE user_id = ? AND status = 'PENDING'", Integer.class, userId);
    assertEquals(1, pending);
  }

  /** 先上传正反面和活体会话，再拼提交报文。 */
  private String kycJson(String token, String userId, String name) throws Exception {
    upload(token, "FRONT", "front-" + name);
    upload(token, "BACK", "back-" + name);
    String session = mvc.perform(post(UserPaths.ME_KYC_LIVENESS).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .split("\"sessionId\":\"")[1]
        .split("\"")[0];
    String proof = proof(session, 90, 91);
    return "{\"fullName\":\"" + name + "\",\"birthDate\":\"1990-01-02\",\"nationality\":\"CN\",\"address\":\"Road 1\","
        + "\"idType\":\"ID_CARD\",\"idNumber\":\"110101199001021234\",\"frontSha256\":\"" + sha(userId, "FRONT")
        + "\",\"backSha256\":\"" + sha(userId, "BACK") + "\",\"livenessSessionId\":\"" + session
        + "\",\"livenessScore\":90,\"faceScore\":91,\"livenessProof\":\"" + proof + "\"}";
  }

  /** 上传一面证件。 */
  private void upload(String token, String side, String text) throws Exception {
    String content = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    mvc.perform(post(UserPaths.ME_KYC_DOCUMENTS)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"side\":\"" + side + "\",\"contentType\":\"text/plain\",\"contentBase64\":\"" + content + "\"}"))
        .andExpect(status().isOk());
  }

  /** 最近一次上传的摘要。 */
  private String sha(String userId, String side) {
    return jdbc.queryForObject(
        "SELECT sha256 FROM kyc_document WHERE user_id = ? AND side = ? ORDER BY created_at DESC LIMIT 1",
        String.class,
        userId,
        side);
  }

  /** 活体证明。 */
  private static String proof(String sessionId, int liveness, int face) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(LIVENESS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String message = sessionId + "|" + liveness + "|" + face;
    return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
  }

  /** 发出验证码并读出本地消息里的明文。 */
  private String code(String channel, String country, String phone, String email, String purpose) throws Exception {
    String json = "SMS".equals(channel)
        ? "{\"channel\":\"SMS\",\"countryCode\":\"" + country + "\",\"phone\":\"" + phone + "\",\"purpose\":\"" + purpose + "\"}"
        : "{\"channel\":\"EMAIL\",\"email\":\"" + email + "\",\"purpose\":\"" + purpose + "\"}";
    mvc.perform(post(AuthPaths.VERIFICATION_CODES).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk());
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
    for (String payload : payloads) {
      String candidate = payload.substring(payload.lastIndexOf('|') + 1);
      if (Digests.sha256(candidate).equals(hash)) {
        return candidate;
      }
    }
    throw new AssertionError("code");
  }

  /** 注册结果。 */
  private record Registered(String userId, String username, String accountNo) {
  }
}
