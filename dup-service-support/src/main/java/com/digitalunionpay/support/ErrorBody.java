package com.digitalunionpay.support;

/** 接口失败时返回的错误体。code 是错误码名，message 是说明。 */
public record ErrorBody(String code, String message) {
}
