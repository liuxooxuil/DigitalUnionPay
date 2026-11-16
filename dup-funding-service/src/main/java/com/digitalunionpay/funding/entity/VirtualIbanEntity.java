package com.digitalunionpay.funding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 虚拟 IBAN，表 funding_virtual_iban。 */
@TableName("funding_virtual_iban")
public class VirtualIbanEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String userId;
  private String currency;
  private String iban;

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

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }
}
