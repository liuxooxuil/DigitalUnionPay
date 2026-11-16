package com.digitalunionpay.physical.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.ledger.TccLedger;
import com.digitalunionpay.physical.mapper.IntakeMapper;
import com.digitalunionpay.physical.service.IntakeService;
import com.digitalunionpay.support.GlobalTransactions;
import com.digitalunionpay.valuation.ValuationPort;
import org.springframework.stereotype.Service;

/** 占位实现。校验入参后抛出 PORT_NOT_IMPLEMENTED，落库走注入的 Mapper。 */
@Service
public final class IntakeServiceImpl implements IntakeService {

  private final ValuationPort valuation;
  private final TccLedger ledger;
  private final IntakeMapper intakes;

  /** 接入协作者和本聚合的 Mapper。 */
  public IntakeServiceImpl(ValuationPort valuation, TccLedger ledger, IntakeMapper intakes) {
    if (valuation == null || ledger == null) {
      throw new IllegalArgumentException("collaborators");
    }
    this.valuation = valuation;
    this.ledger = ledger;
    this.intakes = intakes;
  }

  /** 开启 Seata 全局事务，然后执行入金校验。 */
  @Override
  public String record(String deviceId, String rfid, Money valuationAmount) {
    GlobalTransactions.run("physical.record", () -> recordIntake(deviceId, rfid, valuationAmount));
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "PhysicalIntakePort.record");
  }

  /** 校验入参和协作者，业务尚未实现。 */
  private void recordIntake(String deviceId, String rfid, Money valuationAmount) {
    Ids.required(deviceId, "deviceId");
    Ids.required(rfid, "rfid");
    if (valuationAmount == null) {
      throw new IllegalArgumentException("valuation");
    }
    if (valuation == null || ledger == null) {
      throw new IllegalStateException("collaborators");
    }
    throw new BusinessException(ErrorCode.PORT_NOT_IMPLEMENTED, "PhysicalIntakePort.record");
  }
}
