package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;
import org.springframework.web.client.RestClient;

public final class HttpAddressRegistry implements AddressRegistry {
  private final RestClient client;

  public HttpAddressRegistry(RestClient client) {
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
  public String depositAddress(String userId, String chain) {
    Ids.required(userId, "userId");
    Ids.required(chain, "chain");
    DepositAddress address = body(
        client.get()
            .uri(builder -> builder.path(CryptoPaths.DEPOSIT_ADDRESSES)
                .queryParam("userId", userId)
                .queryParam("chain", chain)
                .build())
            .retrieve()
            .body(DepositAddress.class),
        "depositAddress");
    return address.value();
  }
}
