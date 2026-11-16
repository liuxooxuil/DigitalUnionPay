package com.digitalunionpay.support;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/** 出站 HTTP 按目标服务名做 Sentinel 资源。关闭时直接放行。 */
public final class SentinelGuards {

  /** 禁止实例化。 */
  private SentinelGuards() {
  }

  /** 按目标主机申请 Sentinel 资源，被限流时中断这次出站调用。 */
  public static ClientHttpResponse outbound(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (!AlibabaSwitches.sentinel()) {
      return execution.execute(request, body);
    }
    String resource = request.getURI().getHost();
    if (resource == null || resource.isBlank()) {
      resource = request.getURI().getPath();
    }
    Entry entry = null;
    try {
      entry = SphU.entry(resource);
      return execution.execute(request, body);
    } catch (BlockException ex) {
      throw new BusinessException(ErrorCode.UNAVAILABLE, resource);
    } finally {
      if (entry != null) {
        entry.exit();
      }
    }
  }
}
