package com.digitalunionpay.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalunionpay.support.IdempotencyStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/** 同一范围里的幂等键只能占住一次。 */
@SpringBootTest
@ActiveProfiles("test")
class IdempotencyStoreTest {

  @Autowired
  IdempotencyStore store;

  @Autowired
  Environment environment;

  /** 第一次成功，第二次同一键失败。空白键直接拒绝。 */
  @Test
  void claimIsOncePerScopeAndKey() {
    String key = UUID.randomUUID().toString();
    assertTrue(store.claim("withdrawal", key));
    assertFalse(store.claim("withdrawal", key));
    assertTrue(store.claim("deposit", key));
    assertThrows(IllegalArgumentException.class, () -> store.claim("withdrawal", " "));
    assertEquals("graceful", environment.getProperty("server.shutdown"));
  }
}
