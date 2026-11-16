package com.digitalunionpay.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 检查只有 KYC 通过才允许出入金。 */
class KycStatusTest {

  /** 仅 APPROVED 为 true，其余四种状态都为 false。 */
  @Test
  void onlyApprovedKycAllowsFunding() {
    assertTrue(KycStatus.APPROVED.allowsFunding());
    assertFalse(KycStatus.NOT_SUBMITTED.allowsFunding());
    assertFalse(KycStatus.IN_REVIEW.allowsFunding());
    assertFalse(KycStatus.REJECTED.allowsFunding());
    assertFalse(KycStatus.RFI.allowsFunding());
  }
}
