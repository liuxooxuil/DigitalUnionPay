package com.digitalunionpay.user;

/** 继承案件。通过后由清结算做资金迁移，本接口不直接改余额。 */
public interface InheritanceCasePort {

  /** 立案。资金迁移仍由清结算做。 */
  void open(String caseId, String heirUserId);
}
