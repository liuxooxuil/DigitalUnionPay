package com.digitalunionpay.support;

import com.digitalunionpay.common.AuthPaths;
import com.digitalunionpay.common.error.ErrorCode;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/** 各进程的 Spring Security 链。校验 JWT，并在打开时校验服务签名。不启用表单登录。 */
@Configuration
class AccessSecurity {

  /** 挡住 Boot 自动生成的内存用户。登录不走 UserDetailsService。 */
  @Bean
  @ConditionalOnMissingBean(UserDetailsService.class)
  UserDetailsService accessUsers() {
    return username -> {
      throw new UsernameNotFoundException("username");
    };
  }

  /** 无会话。dup.auth.enabled=false 时不验 JWT。服务签名另有开关。 */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtDecoder decoder,
      ServiceCall calls,
      @Value("${dup.auth.enabled:true}") boolean enabled,
      @Value("${dup.service-auth.enabled:true}") boolean serviceAuth) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors
            .authenticationEntryPoint((request, response, exception) ->
                ErrorResponses.write(response, ErrorCode.UNAUTHENTICATED, "token"))
            .accessDeniedHandler((request, response, exception) ->
                ErrorResponses.write(response, ErrorCode.FORBIDDEN, "path")));
    if (serviceAuth) {
      http.addFilterBefore(new ServiceSignatureFilter(calls, true), AuthorizationFilter.class);
    }
    if (enabled) {
      http.oauth2ResourceServer(oauth2 -> oauth2
          .authenticationEntryPoint((request, response, exception) ->
              ErrorResponses.write(response, ErrorCode.UNAUTHENTICATED, "token"))
          .jwt(jwt -> jwt.decoder(decoder)));
      http.addFilterBefore(new AccessContextFilter(), AuthorizationFilter.class);
    }
    http.authorizeHttpRequests(auth -> {
      auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
      if (!enabled) {
        auth.anyRequest().permitAll();
        return;
      }
      auth.requestMatchers("/error", "/actuator", "/actuator/**").permitAll();
      auth.requestMatchers(
          HttpMethod.POST,
          AuthPaths.REGISTER,
          AuthPaths.LOGIN,
          AuthPaths.ACCESS,
          AuthPaths.VERIFICATION_CODES,
          AuthPaths.LOGIN_CODE,
          AuthPaths.LOGIN_BIOMETRIC,
          AuthPaths.BIOMETRIC_CHALLENGES).permitAll();
      auth.anyRequest().access(new AccessAuthorizer());
    });
    return http.build();
  }
}
