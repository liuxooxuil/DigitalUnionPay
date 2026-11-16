package com.digitalunionpay.user.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 密码用 PBKDF2 保存。存储格式是迭代次数、盐和摘要，用 $ 分开。 */
public final class PasswordHashes {

  private static final int ITERATIONS = 120_000;

  private static final int SALT_BYTES = 16;

  private static final int HASH_BYTES = 32;

  private static final SecureRandom RANDOM = new SecureRandom();

  /** 禁止实例化。 */
  private PasswordHashes() {
  }

  /** 生成带随机盐的密码摘要。 */
  public static String hash(String password) {
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    byte[] digest = pbkdf2(password, salt, ITERATIONS);
    return ITERATIONS + "$" + HexFormat.of().formatHex(salt) + "$" + HexFormat.of().formatHex(digest);
  }

  /** 摘要格式不对或密码不一致时返回 false。 */
  public static boolean matches(String password, String stored) {
    if (password == null || stored == null) {
      return false;
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
