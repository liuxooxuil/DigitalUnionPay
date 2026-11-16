package com.digitalunionpay.support;

/** 本地幂等键。要和业务写入放在同一个数据库事务里，失败回滚后才允许重试。 */
public interface IdempotencyStore {

  /** 第一次占住返回 true，重复提交返回 false。 */
  boolean claim(String scope, String key);
}
