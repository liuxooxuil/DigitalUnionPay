package com.digitalunionpay.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 一张资金账户，表 fund_account，类似一张银行卡。
 * ledgerAccountId 是以后交给账本的 account_id，本表不保存可用余额或冻结余额。
 */
@TableName("fund_account")
public class FundAccountEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String accountNo;

  private String fundCode;

  private String status;

  private String ledgerAccountId;

  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(String accountNo) {
    this.accountNo = accountNo;
  }

  public String getFundCode() {
    return fundCode;
  }

  public void setFundCode(String fundCode) {
    this.fundCode = fundCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLedgerAccountId() {
    return ledgerAccountId;
  }

  public void setLedgerAccountId(String ledgerAccountId) {
    this.ledgerAccountId = ledgerAccountId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
