package com.zhiguan.gujian.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguan.gujian.shared.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证时的统一响应 —— 返回 **401** 且 body 符合 `{code,message,data}` 契约。
 *
 * 修复两个问题：
 *  1. `SecurityConfig` 未配置 EntryPoint 时，Spring Security 6 默认退化为
 *     `Http403ForbiddenEntryPoint`，于是"未登录 / token 过期"返回的是 **403**。
 *     而前端 axios 拦截器只在 **401** 时跳登录 —— 用户会卡在报错页无法被引导重新登录。
 *  2. 401/403 原先**没有任何 body**，前端读不到 `message`，只能显示英文
 *     "Request failed with status code 403"。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                Result.fail(401, "未登录或登录已过期，请重新登录"));
    }
}
