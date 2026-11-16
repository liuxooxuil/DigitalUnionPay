package com.digitalunionpay.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalunionpay.support.TraceIdFilter;
import com.digitalunionpay.user.auth.PasswordHashes;
import com.digitalunionpay.user.entity.RbacUserEntity;
import com.digitalunionpay.user.mapper.RbacUserMapper;
import jakarta.servlet.Filter;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

/** 打开 token 校验后，登录、无 token 和无权限要按 RBAC 表拒绝。 */
@SpringBootTest(properties = "dup.auth.enabled=true")
@ActiveProfiles("test")
class AccessControlTest {

  @Autowired
  WebApplicationContext context;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  RbacUserMapper users;

  @Autowired
  TraceIdFilter traceIdFilter;

  @Autowired
  Filter springSecurityFilterChain;

  private MockMvc mvc;

  /** 清空权限表，并装上链路号和 Spring Security。 */
  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM auth_token");
    jdbc.update("DELETE FROM rbac_role_permission");
    jdbc.update("DELETE FROM rbac_user_role");
    jdbc.update("DELETE FROM rbac_permission");
    jdbc.update("DELETE FROM rbac_role");
    jdbc.update("DELETE FROM rbac_user");
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(traceIdFilter, springSecurityFilterChain).build();
  }

  /** 没有 Authorization 时出入金接口返回 401。 */
  @Test
  void missingTokenIsUnauthorized() throws Exception {
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, "user-1").header(TraceIdFilter.HEADER, "trace-auth"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.traceId").value("trace-auth"));
  }

  /** 有 token 但路径不在权限里时返回 403。 */
  @Test
  void tokenWithoutGrantIsForbidden() throws Exception {
    insertUser("alice", "user-1", "secret");
    grant("user-1", "GET", "/internal/other/**");
    String token = login("alice", "secret");
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, "user-1").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  /** 权限匹配后仍走原来的未实现端口，返回 501。 */
  @Test
  void matchingGrantReachesThePort() throws Exception {
    insertUser("alice", "user-1", "secret");
    grant("user-1", "*", "/internal/**");
    String token = login("alice", "secret");
    mvc.perform(get(UserPaths.FUNDING_ALLOWED, "user-1").header("Authorization", "Bearer " + token))
        .andExpect(status().is(501))
        .andExpect(jsonPath("$.code").value("PORT_NOT_IMPLEMENTED"));
  }

  /** 写入一个可登录的账号。 */
  private void insertUser(String username, String userId, String password) {
    RbacUserEntity user = new RbacUserEntity();
    user.setUsername(username);
    user.setUserId(userId);
    user.setPasswordHash(PasswordHashes.hash(password));
    user.setStatus("ACTIVE");
    users.insert(user);
  }

  /** 给用户挂一个角色和一条接口权限。 */
  private void grant(String userId, String method, String path) {
    jdbc.update("INSERT INTO rbac_role (id, code, name) VALUES (1, 'CLERK', 'clerk')");
    jdbc.update(
        "INSERT INTO rbac_permission (id, code, http_method, path_pattern) VALUES (1, 'ONE', ?, ?)",
        method,
        path);
    jdbc.update("INSERT INTO rbac_user_role (user_id, role_code) VALUES (?, 'CLERK')", userId);
    jdbc.update("INSERT INTO rbac_role_permission (role_code, permission_code) VALUES ('CLERK', 'ONE')");
  }

  /** 登录并取出 JWT。紧凑格式里正好有两处点。 */
  private String login(String username, String password) throws Exception {
    String token = mvc.perform(post("/internal/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .split("\"token\":\"")[1]
        .split("\"")[0];
    assertEquals(2, token.chars().filter(ch -> ch == '.').count());
    return token;
  }
}
