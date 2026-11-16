package com.digitalunionpay.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** 有 Docker 时用 MySQL 跑一遍 Flyway 和管理员种子。没有 Docker 时跳过。 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfDockerAvailable
@Testcontainers(disabledWithoutDocker = true)
class PlatformMysqlTest {

  @Container
  static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

  /** 用容器里的 MySQL 盖过测试剖面的 H2。 */
  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
  }

  @Autowired
  JdbcTemplate jdbc;

  /** 管理员账号由 V3 种子写入。 */
  @Test
  void flywaySeedsTheAdminUser() {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM rbac_user WHERE username = 'admin'", Integer.class);
    assertEquals(1, count);
  }
}
