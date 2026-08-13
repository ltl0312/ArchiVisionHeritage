package com.zhiguan.gujian.shared.config;

import com.zhiguan.gujian.shared.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置 — 无状态 JWT + RBAC 权限模型
 *
 * 权限分层：
 *   /api/v1/auth/**            → 公开（登录注册）
 *   /api/v1/admin/**           → 仅 ADMIN 角色可访问
 *   /api/v1/analysis/zhixi/**  → 公开（古建智析演示）
 *   其余所有 API                → 需登录认证
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize 注解支持
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator 健康检查 + Prometheus 指标（内网监控用）
                .requestMatchers("/actuator/**").permitAll()
                // 公开端点
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/assets/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                .requestMatchers("/api/v1/analysis/zhixi/**").permitAll()
                // 文件上传接口 — 需登录
                .requestMatchers("/api/v1/upload/**").authenticated()
                // 管理员专属端点 — 双重保障：URL 层面拦截 + 方法注解 @PreAuthorize
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // 其余请求需认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
