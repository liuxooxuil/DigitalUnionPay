package com.digitalunionpay.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 出金申请，表 funding_withdrawal。 */
@TableName("funding_withdrawal")
public class WithdrawalEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String userId;
  private Long minorUnits;
  private Integer scale;
  private String currency;
  private String beneficiaryRef;
  private String status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
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

  public String getBeneficiaryRef() {
    return beneficiaryRef;
  }

  public void setBeneficiaryRef(String beneficiaryRef) {
    this.beneficiaryRef = beneficiaryRef;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
