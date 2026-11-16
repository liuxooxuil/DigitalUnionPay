package com.digitalunionpay.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 分录明细，表 ledger_posting。 */
@TableName("ledger_posting")
public class PostingEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private Long journalId;

  private String accountId;

  private String side;

  private Long minorUnits;

  private Integer scale;

  private String currency;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getJournalId() {
    return journalId;
  }

  public void setJournalId(Long journalId) {
    this.journalId = journalId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getSide() {
    return side;
  }

  public void setSide(String side) {
    this.side = side;
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
