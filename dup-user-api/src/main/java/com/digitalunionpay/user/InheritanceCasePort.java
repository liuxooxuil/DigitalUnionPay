package com.digitalunionpay.user;

/** 继承案件。复核通过后在同一笔事务里把可用余额迁到继承人账户。 */
public interface InheritanceCasePort {

  /**
   * 立案。evidenceBase64 由服务端计算哈希，不采信调用方自带的摘要。
   * applicantRole 只能是 HEIR 或 AGENT。
   */
  void open(String caseId, String estateUserId, String heirUserId, String applicantRole, String evidenceBase64);
}
