package com.digitalunionpay.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 用户档案，表 user_profile。 */
@TableName("user_profile")
public class UserProfileEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String userId;

  private String userStatus;

  private String kycStatus;

  private String accountStatus;

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

  public String getUserStatus() {
    return userStatus;
  }

  public void setUserStatus(String userStatus) {
    this.userStatus = userStatus;
  }

  public String getKycStatus() {
    return kycStatus;
  }

  public void setKycStatus(String kycStatus) {
    this.kycStatus = kycStatus;
  }

  public String getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
  }
}
