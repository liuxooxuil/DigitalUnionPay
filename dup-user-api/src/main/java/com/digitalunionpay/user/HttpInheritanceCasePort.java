package com.digitalunionpay.user;

import org.springframework.web.client.RestClient;

public final class HttpInheritanceCasePort implements InheritanceCasePort {
  private final RestClient client;

  public HttpInheritanceCasePort(RestClient client) {
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
  public void open(String caseId, String heirUserId) {
    client.post()
        .uri(UserPaths.INHERITANCE_CASES)
        .body(new OpenInheritanceCase(caseId, heirUserId))
        .retrieve()
        .toBodilessEntity();
  }
}
