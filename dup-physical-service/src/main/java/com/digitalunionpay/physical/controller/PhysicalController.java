package com.digitalunionpay.physical.controller;

import com.digitalunionpay.physical.IntakeId;
import com.digitalunionpay.physical.PhysicalPaths;
import com.digitalunionpay.physical.RecordIntake;
import com.digitalunionpay.physical.service.IntakeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class PhysicalController {

  private final IntakeService intake;

  /** 接入本进程的端口实现。 */
  public PhysicalController(IntakeService intake) {
    this.intake = intake;
  }

  /** 处理 POST /internal/physical/intakes，返回入金号。 */
  @PostMapping(PhysicalPaths.INTAKES)
  public IntakeId record(@RequestBody RecordIntake request) {
    return new IntakeId(intake.record(request.deviceId(), request.rfid(), request.valuation()));
  }
}
