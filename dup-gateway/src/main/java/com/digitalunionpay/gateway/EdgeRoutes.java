package com.digitalunionpay.gateway;

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
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.json.JsonMapper;

/** 对外转发用户模块。转发前改写成内部路径并带上服务间 HMAC。下游不可达时返回 503。 */
@Configuration
class EdgeRoutes {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** 注册、登录、会话、KYC、账户和继承都转到用户服务。 */
  @Bean
  RouterFunction<ServerResponse> userRoutes(
      @Value("${dup.user-service-uri}") String userService,
      @Value("${dup.service-auth.secret}") String serviceSecret,
      Clock clock) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    return RouterFunctions.route(
        EdgeRoutes::forwardable,
        request -> forward(request, http, userService, serviceSecret, clock));
  }

  /** 只转发用户模块的对外路径。探测接口留在网关本地。 */
  private static boolean forwardable(ServerRequest request) {
    String path = request.path();
    return path.startsWith("/api/auth/")
        || path.startsWith("/api/users/")
        || "/api/users".equals(path)
        || path.startsWith("/api/sessions/")
        || "/api/sessions".equals(path)
        || path.startsWith("/api/inheritance-cases/")
        || "/api/inheritance-cases".equals(path)
        || path.startsWith("/api/kyc-cases/")
        || "/api/kyc-cases".equals(path)
        || path.startsWith("/api/closures/")
        || "/api/closures".equals(path);
  }

  /** 读出报文体，签内部路径，再发给用户服务。连接失败不是业务成功。 */
  private static ServerResponse forward(
      ServerRequest request, HttpClient http, String userService, String serviceSecret, Clock clock) {
    String external = request.path();
    String internal = EdgeGate.grantPath(external);
    byte[] body;
    try {
      body = request.servletRequest().getInputStream().readAllBytes();
    } catch (java.io.IOException exception) {
      return unavailable();
    }
    String timestamp = Long.toString(clock.millis());
    String nonce = UUID.randomUUID().toString();
    String method = request.method().name();
    String sign = EdgeSigner.sign(serviceSecret, method, internal, timestamp, nonce, body);
    String query = request.uri().getRawQuery();
    String target = trim(userService) + internal + (query == null || query.isBlank() ? "" : "?" + query);
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target))
        .timeout(Duration.ofSeconds(2))
        .header("X-Service-Time", timestamp)
        .header("X-Service-Nonce", nonce)
        .header("X-Service-Sign", sign);
    copy(builder, request, "Content-Type");
    copy(builder, request, "Authorization");
    copy(builder, request, "X-Trace-Id");
    String forwarded = request.headers().firstHeader("X-Forwarded-For");
    if (forwarded == null || forwarded.isBlank()) {
      forwarded = request.servletRequest().getRemoteAddr();
    }
    if (forwarded != null && !forwarded.isBlank()) {
      builder.header("X-Forwarded-For", forwarded);
    }
    HttpRequest outbound = builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body)).build();
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

  /** 把调用方的头继续带给用户服务。没有就不加。 */
  private static void copy(HttpRequest.Builder builder, ServerRequest request, String name) {
    String value = request.headers().firstHeader(name);
    if (value != null && !value.isBlank()) {
      builder.header(name, value);
    }
  }

  /** 下游没接上。发送失败不表示业务完成。 */
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
