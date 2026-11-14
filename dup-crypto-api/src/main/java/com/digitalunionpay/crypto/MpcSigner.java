package com.digitalunionpay.crypto;

/** 托管签名边界。部署约束是 3-of-5 阈值签名，再加 HSM 二次确认。 */
public interface MpcSigner {

  String sign(String payloadRef);
}
