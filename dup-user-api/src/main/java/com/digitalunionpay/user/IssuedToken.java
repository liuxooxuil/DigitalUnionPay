package com.digitalunionpay.user;

import java.time.LocalDateTime;

/** 登录成功后交给调用方的访问 token。expiresAt 是绝对过期时间，UTC。sessionId 用来远程下线。 */
public record IssuedToken(String token, String userId, LocalDateTime expiresAt, Long sessionId) {
}
