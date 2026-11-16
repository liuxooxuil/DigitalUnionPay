package com.digitalunionpay.physical;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

/** 调用 physical-service:8090 记录实物入金。 */
public final class HttpPhysicalIntakePort implements PhysicalIntakePort {
  private final RestClient client;

  /** 保存已带负载均衡和链路号的客户端。 */
  public HttpPhysicalIntakePort(RestClient client) {
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

  /** POST /internal/physical/intakes，提交设备、RFID 和估值。 */
  @Override
  public String record(String deviceId, String rfid, Money valuation) {
    IntakeId id = body(
        client.post()
            .uri(PhysicalPaths.INTAKES)
            .body(new RecordIntake(deviceId, rfid, valuation))
            .retrieve()
            .body(IntakeId.class),
        "intakeId");
    return id.value();
  }
}
