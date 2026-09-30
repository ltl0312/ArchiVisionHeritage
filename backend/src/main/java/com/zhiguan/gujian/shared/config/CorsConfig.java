package com.zhiguan.gujian.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS 配置。
 *
 * 安全修复：原实现是 `addAllowedOriginPattern("*")` + `setAllowCredentials(true)` ——
 * 这是被明确警示的危险组合：允许**任意站点**携带凭据跨域调用 API。
 * 当前鉴权走 `Authorization: Bearer` 头，攻击面有限，但一旦将来引入 cookie 会话，
 * 立即变成凭据泄露 / CSRF 洞。
 *
 * 现在改为**显式来源白名单**（`addAllowedOrigin` 精确匹配，可与 allowCredentials 共存），
 * 并允许通过 `zhiguan.cors.allowed-origins` 覆盖。
 *
 * 说明：本项目的开发（Vite 代理）与生产（nginx 反代）都是同源访问 `/api`，
 * 正常情况下根本不会触发 CORS；白名单只是为"浏览器直连后端"的场景兜底。
 */
@Configuration
public class CorsConfig {

    /** 逗号分隔的允许来源；默认只放行本机开发/验证端口 */
    @Value("${zhiguan.cors.allowed-origins:"
            + "http://localhost:5173,http://127.0.0.1:5173,"
            + "http://localhost:8088,http://127.0.0.1:8088}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(config::addAllowedOrigin);

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        // 精确来源 + 允许凭据：这是唯一安全的组合（通配来源时浏览器本身也会拒绝）
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
