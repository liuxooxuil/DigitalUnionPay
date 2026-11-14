package com.digitalunionpay.crypto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CryptoController {

  private final AddressRegistry addresses;
  private final MpcSigner signer;

  public CryptoController(AddressRegistry addresses, MpcSigner signer) {
    this.addresses = addresses;
    this.signer = signer;
  }

  @GetMapping(CryptoPaths.DEPOSIT_ADDRESSES)
  public DepositAddress depositAddress(@RequestParam("userId") String userId, @RequestParam("chain") String chain) {
    return new DepositAddress(addresses.depositAddress(userId, chain));
  }

  @PostMapping(CryptoPaths.SIGNATURES)
  public SignatureRef sign(@RequestBody SignRequest request) {
    return new SignatureRef(signer.sign(request.payloadRef()));
  }
}
