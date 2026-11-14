package com.digitalunionpay.common;

public final class Ids {

  private Ids() {
  }

  public static String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name);
    }
    return value;
  }
}
