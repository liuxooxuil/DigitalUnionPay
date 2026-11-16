package com.digitalunionpay.gateway;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.common.result.ApiResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.json.JsonMapper;

/** 唯一对外转发的是登录。转发前改写成内部路径并带上服务间 HMAC。下游不可达时返回 503。 */
@Configuration
class EdgeRoutes {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** 登录路由。用户服务地址在测试里指向一个没有进程的端口。 */
  @Bean
  RouterFunction<ServerResponse> loginRoute(
      @Value("${dup.user-service-uri}") String userService,
      @Value("${dup.service-auth.secret}") String serviceSecret,
      Clock clock) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    return route("user-login").POST("/api/auth/login", request -> forward(request, http, userService, serviceSecret, clock)).build();
  }

  /** 读出报文体，签内部路径，再发给用户服务。连接失败不是登录成功。 */
  private static ServerResponse forward(
      ServerRequest request, HttpClient http, String userService, String serviceSecret, Clock clock) {
    byte[] body;
    try {
      body = request.servletRequest().getInputStream().readAllBytes();
    } catch (java.io.IOException exception) {
      return unavailable();
    }
    String path = "/internal/auth/login";
    String timestamp = Long.toString(clock.millis());
    String nonce = UUID.randomUUID().toString();
    String sign = EdgeSigner.sign(serviceSecret, "POST", path, timestamp, nonce, body);
    HttpRequest outbound = HttpRequest.newBuilder(URI.create(trim(userService) + path))
        .timeout(Duration.ofSeconds(2))
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .header("X-Service-Time", timestamp)
        .header("X-Service-Nonce", nonce)
        .header("X-Service-Sign", sign)
        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        .build();
    try {
      HttpResponse<byte[]> downstream = http.send(outbound, HttpResponse.BodyHandlers.ofByteArray());
      byte[] responseBody = downstream.body() == null ? new byte[0] : downstream.body();
      return ServerResponse.status(downstream.statusCode())
          .contentType(MediaType.APPLICATION_JSON)
          .body(new String(responseBody, StandardCharsets.UTF_8));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return unavailable();
    } catch (Exception exception) {
      return unavailable();
    }
  }

  /** 下游没接上。发送失败不表示登录完成。 */
  private static ServerResponse unavailable() {
    String json = JSON.writeValueAsString(ApiResult.fail(ErrorCode.UNAVAILABLE.name(), "user-service", ""));
    return ServerResponse.status(EdgeResponses.status(ErrorCode.UNAVAILABLE))
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }

  /** 去掉配置末尾的斜杠，避免拼出双斜杠。 */
  private static String trim(String userService) {
    if (userService == null || userService.isBlank()) {
      return "http://127.0.0.1:8081";
    }
    return userService.endsWith("/") ? userService.substring(0, userService.length() - 1) : userService;
  }
}
