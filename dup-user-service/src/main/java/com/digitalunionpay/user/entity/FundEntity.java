package com.digitalunionpay.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 一种可单独开户的资金，表 fund。注册不写这张表，余额也不在这里。 */
@TableName("fund")
public class FundEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String fundCode;

  private String name;

  private String currency;

  private Integer scale;

  private String status;

  private String fundKind;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFundCode() {
    return fundCode;
  }

  public void setFundCode(String fundCode) {
    this.fundCode = fundCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getScale() {
    return scale;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFundKind() {
    return fundKind;
  }

  public void setFundKind(String fundKind) {
    this.fundKind = fundKind;
  }
}
