package com.digitalunionpay.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM。密钥来自配置，不是 HSM，也不能代替托管门限签名。 */
public final class FieldCipher {

  private static final int IV_BYTES = 12;

  private static final int TAG_BITS = 128;

  private final byte[] key;

  private final SecureRandom random = new SecureRandom();

  /** 密钥必须是 32 字节。 */
  public FieldCipher(byte[] key) {
    if (key == null || key.length != 32) {
      throw new IllegalArgumentException("key");
    }
    this.key = key.clone();
  }

  /** 明文加密成 base64。前面 12 字节是 IV。 */
  public String encrypt(String plain) {
    if (plain == null) {
      throw new IllegalArgumentException("plain");
    }
    byte[] iv = new byte[IV_BYTES];
    random.nextBytes(iv);
    byte[] body = crypt(Cipher.ENCRYPT_MODE, iv, plain.getBytes(StandardCharsets.UTF_8));
    byte[] packed = new byte[iv.length + body.length];
    System.arraycopy(iv, 0, packed, 0, iv.length);
    System.arraycopy(body, 0, packed, iv.length, body.length);
    return Base64.getEncoder().encodeToString(packed);
  }

  /** 解回明文。格式不对时拒绝。 */
  public String decrypt(String packed) {
    if (packed == null || packed.isBlank()) {
      throw new IllegalArgumentException("packed");
    }
    byte[] raw = Base64.getDecoder().decode(packed);
    if (raw.length <= IV_BYTES) {
      throw new IllegalArgumentException("packed");
    }
    byte[] iv = new byte[IV_BYTES];
    byte[] body = new byte[raw.length - IV_BYTES];
    System.arraycopy(raw, 0, iv, 0, IV_BYTES);
    System.arraycopy(raw, IV_BYTES, body, 0, body.length);
    return new String(crypt(Cipher.DECRYPT_MODE, iv, body), StandardCharsets.UTF_8);
  }

  /** 用同一把 AES-256 密钥做 GCM。 */
  private byte[] crypt(int mode, byte[] iv, byte[] input) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
      return cipher.doFinal(input);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("cipher");
    }
  }
}
