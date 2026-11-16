package com.digitalunionpay.crypto;

import org.springframework.web.client.RestClient;

/** 调用 crypto-service:8089 提交托管签名。 */
public final class HttpMpcSigner implements MpcSigner {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpMpcSigner(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  /** 响应体为空时失败，避免把空结果当成成功。 */
  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  /** POST /internal/crypto/signatures，提交载荷引用并取回签名引用。 */
  @Override
  public String sign(String payloadRef) {
    SignatureRef signature = body(
        client.post().uri(CryptoPaths.SIGNATURES).body(new SignRequest(payloadRef)).retrieve().body(SignatureRef.class),
        "signature");
    return signature.value();
  }
}
