package com.digitalunionpay.physical;

import com.digitalunionpay.common.money.Money;
import org.springframework.web.client.RestClient;

public final class HttpPhysicalIntakePort implements PhysicalIntakePort {
  private final RestClient client;

  public HttpPhysicalIntakePort(RestClient client) {
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
