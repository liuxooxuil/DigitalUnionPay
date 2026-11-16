package com.digitalunionpay.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.digitalunionpay.common.money.Money;
import com.digitalunionpay.common.page.PageQuery;
import com.digitalunionpay.common.page.PageResult;
import com.digitalunionpay.common.result.ApiResult;
import com.digitalunionpay.common.time.Utc;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 金额、分页、时间和失败外壳这些底座规则。 */
class FoundationTest {

  /** 同一币种才能相加，币种或 scale 不同、以及溢出都拒绝。 */
  @Test
  void moneyAddsOnlyMatchingCurrencyAndScale() {
    Money sum = new Money(1000, 2, "MYR").plus(new Money(250, 2, "MYR"));
    assertEquals(1250, sum.minorUnits());
    assertThrows(IllegalArgumentException.class, () -> new Money(1, 2, "MYR").plus(new Money(1, 2, "USD")));
    assertThrows(IllegalArgumentException.class, () -> new Money(1, 2, "MYR").plus(new Money(1, 0, "MYR")));
    assertThrows(IllegalArgumentException.class, () -> new Money(Long.MAX_VALUE, 0, "MYR").plus(new Money(1, 0, "MYR")));
  }

  /** 页码从 1 开始，偏移按页长计算。 */
  @Test
  void pageQueryRejectsOutOfRangeValues() {
    assertEquals(20, new PageQuery(2, 20).offset());
    assertThrows(IllegalArgumentException.class, () -> new PageQuery(0, 20));
    assertThrows(IllegalArgumentException.class, () -> new PageQuery(1, 101));
    PageResult<String> page = new PageResult<>(List.of("a"), 1, 1, 20);
    assertEquals(List.of("a"), page.items());
  }

  /** 失败外壳不带数据，空白消息变成空字符串。 */
  @Test
  void failureResultDropsThePayload() {
    ApiResult<String> result = ApiResult.fail("CONFLICT", null, "trace-1");
    assertEquals("CONFLICT", result.code());
    assertEquals("", result.message());
    assertNull(result.data());
    assertEquals("trace-1", result.traceId());
  }

  /** 空时钟时仍取 UTC。 */
  @Test
  void utcNowUsesTheGivenClock() {
    Clock fixed = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC);
    assertEquals(Instant.parse("2026-01-02T03:04:05Z"), Utc.now(fixed));
  }
}
