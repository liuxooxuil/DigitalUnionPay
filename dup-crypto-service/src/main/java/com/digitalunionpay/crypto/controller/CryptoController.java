package com.digitalunionpay.crypto.controller;

import com.digitalunionpay.crypto.CryptoPaths;
import com.digitalunionpay.crypto.DepositAddress;
import com.digitalunionpay.crypto.SignRequest;
import com.digitalunionpay.crypto.SignatureRef;
import com.digitalunionpay.crypto.service.CustodySignService;
import com.digitalunionpay.crypto.service.DepositAddressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 本服务的内部 HTTP 入口。 */
@RestController
public class CryptoController {

  private final DepositAddressService addresses;
  private final CustodySignService signer;

  /** 接入本进程的端口实现。 */
  public CryptoController(DepositAddressService addresses, CustodySignService signer) {
    this.addresses = addresses;
    this.signer = signer;
  }

  /** 处理 GET /internal/crypto/deposit-addresses，返回充值地址。 */
  @GetMapping(CryptoPaths.DEPOSIT_ADDRESSES)
  public DepositAddress depositAddress(@RequestParam("userId") String userId, @RequestParam("chain") String chain) {
    return new DepositAddress(addresses.depositAddress(userId, chain));
  }

  /** 处理 POST /internal/crypto/signatures，返回签名引用。 */
  @PostMapping(CryptoPaths.SIGNATURES)
  public SignatureRef sign(@RequestBody SignRequest request) {
    return new SignatureRef(signer.sign(request.payloadRef()));
  }
}
