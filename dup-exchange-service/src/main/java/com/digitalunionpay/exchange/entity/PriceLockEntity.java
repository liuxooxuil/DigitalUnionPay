package com.digitalunionpay.exchange.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/** 锁价，表 exchange_price_lock。 */
@TableName("exchange_price_lock")
public class PriceLockEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String lockId;
  private String userId;
  private Long sourceMinorUnits;
  private Integer scale;
  private String sourceCurrency;
  private String targetCurrency;
  private Instant expiresAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLockId() {
    return lockId;
  }

  public void setLockId(String lockId) {
    this.lockId = lockId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Long getSourceMinorUnits() {
    return sourceMinorUnits;
  }

  public void setSourceMinorUnits(Long sourceMinorUnits) {
    this.sourceMinorUnits = sourceMinorUnits;
  }

  public Integer getScale() {
    return scale;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public String getSourceCurrency() {
    return sourceCurrency;
  }

  public void setSourceCurrency(String sourceCurrency) {
    this.sourceCurrency = sourceCurrency;
  }

  public String getTargetCurrency() {
    return targetCurrency;
  }

  public void setTargetCurrency(String targetCurrency) {
    this.targetCurrency = targetCurrency;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }
}
