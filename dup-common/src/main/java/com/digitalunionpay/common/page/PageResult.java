package com.digitalunionpay.common.page;

import java.util.List;

/** 一页数据和总行数。items 会复制一份，调用方之后改自己的列表不影响结果。 */
public record PageResult<T>(List<T> items, long total, int page, int size) {

  /** 拒绝负数总行数和非法页参数。 */
  public PageResult {
    if (total < 0) {
      throw new IllegalArgumentException("total");
    }
    items = items == null ? List.of() : List.copyOf(items);
    PageQuery check = new PageQuery(page, size);
    page = check.page();
    size = check.size();
  }
}
