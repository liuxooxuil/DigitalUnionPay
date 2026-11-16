package com.digitalunionpay.common.page;

/** 页码从 1 开始，单页最多 100 条。列表接口共用这一组边界。 */
public record PageQuery(int page, int size) {

  /** 拒绝第 0 页和过大的页长。 */
  public PageQuery {
    if (page < 1) {
      throw new IllegalArgumentException("page");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("size");
    }
  }

  /** 换算成 SQL 的偏移。第一页偏移是 0。 */
  public long offset() {
    return (long) (page - 1) * size;
  }
}
