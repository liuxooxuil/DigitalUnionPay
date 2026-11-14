package com.digitalunionpay.funding;

import org.springframework.web.client.RestClient;

public final class HttpFiatCollectionPort implements FiatCollectionPort {
  private final RestClient client;

  public HttpFiatCollectionPort(RestClient client) {
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
  public String issueVirtualIban(String userId, String currency) {
    IssuedIban issued = body(
        client.post().uri(FundingPaths.VIRTUAL_IBANS).body(new IssueVirtualIban(userId, currency)).retrieve().body(IssuedIban.class),
        "issuedIban");
    return issued.value();
  }
}
