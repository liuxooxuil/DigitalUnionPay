package com.digitalunionpay.crypto;

import com.digitalunionpay.common.Ids;
import org.springframework.web.client.RestClient;

/** 调用 crypto-service:8089 查询充值地址。 */
public final class HttpAddressRegistry implements AddressRegistry {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpAddressRegistry(RestClient client) {
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

  /** GET /internal/crypto/deposit-addresses，按用户和链取充值地址。 */
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
