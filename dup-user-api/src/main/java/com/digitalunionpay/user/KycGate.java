package com.digitalunionpay.user;

/** 出入金和兑换前查询 KYC 是否已通过。 */
public interface KycGate {

  /** 查询该用户当前是否允许出入金。 */
  boolean fundingAllowed(String userId);
}
