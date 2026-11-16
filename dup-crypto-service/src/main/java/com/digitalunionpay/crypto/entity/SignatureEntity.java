package com.digitalunionpay.crypto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 表 crypto_signature。 */
@TableName("crypto_signature")
public class SignatureEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String payloadRef;

  private String signatureRef;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPayloadRef() {
    return payloadRef;
  }

  public void setPayloadRef(String payloadRef) {
    this.payloadRef = payloadRef;
  }

  public String getSignatureRef() {
    return signatureRef;
  }

  public void setSignatureRef(String signatureRef) {
    this.signatureRef = signatureRef;
  }
}
