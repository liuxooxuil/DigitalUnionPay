package com.digitalunionpay.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 继承案件，表 inheritance_case。 */
@TableName("inheritance_case")
public class InheritanceCaseEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String caseId;

  private String heirUserId;

  private String status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  public String getHeirUserId() {
    return heirUserId;
  }

  public void setHeirUserId(String heirUserId) {
    this.heirUserId = heirUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
