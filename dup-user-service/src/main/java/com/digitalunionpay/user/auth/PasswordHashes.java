package com.digitalunionpay.user.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** 新密码用 Argon2id。已有的 PBKDF2 摘要仍能校验，种子管理员不用重新哈希。 */
public final class PasswordHashes {

  private static final int HASH_BYTES = 32;

  private static final Argon2PasswordEncoder ARGON = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

  /** 禁止实例化。 */
  private PasswordHashes() {
  }

  /** 生成 Argon2id 摘要。 */
  public static String hash(String password) {
    if (password == null) {
      throw new IllegalArgumentException("password");
    }
    return ARGON.encode(password);
  }

  /** Argon2id 走新格式。三段 iterations$salt$hash 仍按 PBKDF2 校验。 */
  public static boolean matches(String password, String stored) {
    if (password == null || stored == null) {
      return false;
    }
    if (stored.startsWith("$argon2id$")) {
      return ARGON.matches(password, stored);
    }
    String[] parts = stored.split("\\$");
    if (parts.length != 3) {
      return false;
    }
    int iterations;
    try {
      iterations = Integer.parseInt(parts[0]);
    } catch (NumberFormatException exception) {
      return false;
    }
    if (iterations < 1 || iterations > 1_000_000) {
      return false;
    }
    byte[] salt = HexFormat.of().parseHex(parts[1]);
    byte[] expected = HexFormat.of().parseHex(parts[2]);
    byte[] actual = pbkdf2(password, salt, iterations);
    return MessageDigest.isEqual(actual, expected);
  }

  /** 按给定盐和迭代次数计算摘要。 */
  private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BYTES * 8);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("password");
    }
  }
}
