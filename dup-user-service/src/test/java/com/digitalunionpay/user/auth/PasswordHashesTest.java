package com.digitalunionpay.user.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 同一密码能对上自己的摘要，对不上别的密码。 */
class PasswordHashesTest {

  /** 摘要每次盐不同，但校验仍然通过。 */
  @Test
  void matchesThePasswordThatWasHashed() {
    String stored = PasswordHashes.hash("secret");
    assertTrue(stored.startsWith("$argon2id$"));
    assertTrue(PasswordHashes.matches("secret", stored));
    assertFalse(PasswordHashes.matches("other", stored));
    assertFalse(PasswordHashes.matches("secret", "not-a-hash"));
  }

  /** 种子数据里的管理员摘要必须对上初始密码。 */
  @Test
  void seedAdminPasswordMatches() {
    String stored = "120000$c22b60f3add6d1a293713a3875ae534d$fef896047fb8c0445e63c9349489fd9d99c23acd50b65a5e46ee33b63aa8f19a";
    assertTrue(PasswordHashes.matches("admin123", stored));
    assertFalse(PasswordHashes.matches("root", stored));
  }
}
