package com.digitalunionpay.user;

/** 要检查的目标方法和路径。路径不含 context path。 */
public record AccessCheck(String method, String path) {
}
