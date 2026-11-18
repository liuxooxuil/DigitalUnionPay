package com.digitalunionpay.user.service;

import com.digitalunionpay.user.InheritanceCasePort;

/** 继承立案和双人复核。 */
public interface InheritanceCaseService extends InheritanceCasePort {

  /** 初审把案件推进到复核中，复审通过后冻结原账户并迁移可用余额。 */
  CustomerViews.InheritanceView review(String caseId, String reviewerId, boolean approve, String reason);
}
