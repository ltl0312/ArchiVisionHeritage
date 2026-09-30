package com.zhiguan.gujian.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguan.gujian.shared.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已认证但无权限时的统一响应 —— 返回 **403** 且 body 符合 `{code,message,data}` 契约。
 *
 * 原先 403 是 Spring Security 的默认空响应体，前端 `error.response.data.message`
 * 取不到值，只能退化成英文提示。
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                Result.fail(403, "无权限访问该资源"));
    }
}
