package com.digitalunionpay.gateway;

import java.util.List;
import org.springframework.util.AntPathMatcher;

/** JWT 里的接口权限。一条权限是「方法 空格 路径」，与用户服务写入的 grants 相同。 */
final class EdgeGrants {

  private static final AntPathMatcher PATHS = new AntPathMatcher();

  private EdgeGrants() {
  }

  /** * 表示不限方法。路径用 Ant 风格。声明不是列表时拒绝。 */
  static boolean allows(Object claim, String method, String path) {
    if (!(claim instanceof List<?> grants) || method == null || path == null) {
      return false;
    }
    for (Object item : grants) {
      if (!(item instanceof String text)) {
        continue;
      }
      int space = text.indexOf(' ');
      if (space < 1 || space >= text.length() - 1) {
        continue;
      }
      String granted = text.substring(0, space);
      String pattern = text.substring(space + 1);
      if (("*".equals(granted) || granted.equalsIgnoreCase(method)) && PATHS.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }
}
