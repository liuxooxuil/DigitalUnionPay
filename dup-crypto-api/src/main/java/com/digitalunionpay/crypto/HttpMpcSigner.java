package com.digitalunionpay.crypto;

import org.springframework.web.client.RestClient;

public final class HttpMpcSigner implements MpcSigner {
  private final RestClient client;

  public HttpMpcSigner(RestClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client");
    }
    this.client = client;
  }

  private static <T> T body(T value, String name) {
    if (value == null) {
      throw new IllegalStateException(name);
    }
    return value;
  }

  @Override
  public String sign(String payloadRef) {
    SignatureRef signature = body(
        client.post().uri(CryptoPaths.SIGNATURES).body(new SignRequest(payloadRef)).retrieve().body(SignatureRef.class),
        "signature");
    return signature.value();
  }
}
