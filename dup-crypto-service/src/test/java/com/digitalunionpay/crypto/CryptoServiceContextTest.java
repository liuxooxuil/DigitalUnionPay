package com.digitalunionpay.crypto;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.digitalunionpay.crypto.service.impl.CustodySignServiceImpl;
import com.digitalunionpay.crypto.service.impl.DepositAddressServiceImpl;
import com.digitalunionpay.ledger.HttpTccLedger;
import com.digitalunionpay.ledger.TccLedger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 检查加密资产服务上下文：地址和签名是骨架，清结算是 HTTP 客户端。 */
@SpringBootTest
@ActiveProfiles("test")
class CryptoServiceContextTest {

  @Autowired
  AddressRegistry addresses;

  @Autowired
  MpcSigner signer;

  @Autowired
  TccLedger ledger;

  /** 断言本进程端口为骨架，清结算为 HttpTccLedger。 */
  @Test
  void wiresThisService() {
    assertInstanceOf(DepositAddressServiceImpl.class, addresses);
    assertInstanceOf(CustodySignServiceImpl.class, signer);
    assertInstanceOf(HttpTccLedger.class, ledger);
  }
}
