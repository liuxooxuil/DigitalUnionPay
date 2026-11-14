package com.digitalunionpay.user;

import com.digitalunionpay.common.Ids;
import org.springframework.web.client.RestClient;

public final class HttpKycGate implements KycGate {
  private final RestClient client;

  public HttpKycGate(RestClient client) {
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
  public boolean fundingAllowed(String userId) {
    Ids.required(userId, "userId");
    return Boolean.TRUE.equals(client.get().uri(UserPaths.FUNDING_ALLOWED, userId).retrieve().body(Boolean.class));
  }
}
