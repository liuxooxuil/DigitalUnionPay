package com.digitalunionpay.user;

/** 注册完成后的用户。accountNo 是余额为 0 的主账户号，状态在 userStatus 里为待 KYC。 */
public record RegisteredUser(String userId, String username, String userStatus, String kycStatus, String accountNo) {
}
