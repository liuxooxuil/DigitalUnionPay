package com.digitalunionpay.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KycStatusTest {

  @Test
  void onlyApprovedKycAllowsFunding() {
    assertTrue(KycStatus.APPROVED.allowsFunding());
    assertFalse(KycStatus.NOT_SUBMITTED.allowsFunding());
    assertFalse(KycStatus.IN_REVIEW.allowsFunding());
    assertFalse(KycStatus.REJECTED.allowsFunding());
    assertFalse(KycStatus.RFI.allowsFunding());
  }
}
