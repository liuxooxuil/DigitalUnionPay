package com.digitalunionpay.user;

import java.time.LocalDateTime;

/** 登录成功后交给调用方的访问 token。expiresAt 是 UTC。 */
public record IssuedToken(String token, String userId, LocalDateTime expiresAt) {
}
