package com.digitalunionpay.gateway;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 边缘本地探测。不转发到任何资金服务。过滤器已经验过签名和权限。 */
@RestController
class EdgePingController {

  /** 进程活着就返回 up。 */
  @GetMapping("/api/edge/ping")
  Map<String, String> ping() {
    return Map.of("status", "up");
  }
}
