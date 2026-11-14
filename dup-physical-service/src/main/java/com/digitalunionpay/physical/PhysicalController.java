package com.digitalunionpay.physical;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PhysicalController {

  private final PhysicalIntakePort intake;

  public PhysicalController(PhysicalIntakePort intake) {
    this.intake = intake;
  }

  @PostMapping(PhysicalPaths.INTAKES)
  public IntakeId record(@RequestBody RecordIntake request) {
    return new IntakeId(intake.record(request.deviceId(), request.rfid(), request.valuation()));
  }
}
