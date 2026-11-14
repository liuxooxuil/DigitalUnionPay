package com.digitalunionpay.user;

/** 出入金和兑换前查询 KYC 是否已通过。 */
public interface KycGate {

  boolean fundingAllowed(String userId);
}
