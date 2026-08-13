package com.zhiguan.gujian.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguan.gujian.dto.request.LoginRequest;
import com.zhiguan.gujian.dto.request.RegisterRequest;
import com.zhiguan.gujian.exception.CulturalApiException;
import com.zhiguan.gujian.exception.GlobalExceptionHandler;
import com.zhiguan.gujian.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证控制器测试")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("登录成功 - 返回 token")
    void login_success_returnsToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn("jwt.token.here");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("test123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt.token.here"));
    }

    @Test
    @DisplayName("登录失败 - 用户名或密码错误")
    void login_failure_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new CulturalApiException(401, "用户名或密码错误"));

        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser");
        request.setPassword("wrongpass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("注册成功")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("new123");
        request.setNickname("新用户");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_failure_usernameExists() throws Exception {
        doThrow(new CulturalApiException(400, "用户名已被注册"))
                .when(authService).register(any(RegisterRequest.class));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("test123");
        request.setNickname("测试");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已被注册"));
    }
}
