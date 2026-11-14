package com.digitalunionpay.crypto;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.crypto.internal.SkeletonAddressRegistry;
import com.digitalunionpay.crypto.internal.SkeletonMpcSigner;
import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CryptoServiceContextTest {

  @Autowired
  AddressRegistry addresses;

  @Autowired
  MpcSigner signer;

  @Autowired
  TccLedger ledger;

  @Test
  void wiresThisService() {
    assertInstanceOf(SkeletonAddressRegistry.class, addresses);
    assertInstanceOf(SkeletonMpcSigner.class, signer);
    assertInstanceOf(HttpTccLedger.class, ledger);
  }
}
