package com.digitalunionpay.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 冻结，表 ledger_freeze。 */
@TableName("ledger_freeze")
public class FreezeEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String traceId;

  private String reference;

  private String accountId;

  private Long minorUnits;

  private Integer scale;

  private String currency;

  private String status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public Long getMinorUnits() {
    return minorUnits;
  }

  public void setMinorUnits(Long minorUnits) {
    this.minorUnits = minorUnits;
  }

  public Integer getScale() {
    return scale;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
