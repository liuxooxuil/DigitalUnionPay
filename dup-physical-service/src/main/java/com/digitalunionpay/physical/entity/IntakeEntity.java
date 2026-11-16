package com.digitalunionpay.physical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 表 physical_intake。 */
@TableName("physical_intake")
public class IntakeEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String deviceId;

  private String rfid;

  private Long minorUnits;

  private Integer scale;

  private String currency;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getRfid() {
    return rfid;
  }

  public void setRfid(String rfid) {
    this.rfid = rfid;
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
}
