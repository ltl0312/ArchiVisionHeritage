package com.zhiguan.gujian.auth.interfaces;

import com.zhiguan.gujian.auth.application.UserApplicationService;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.shared.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProfileController 单元测试 — 个人中心接口（自 CommunityController 迁出，URL 契约不变）
 *
 * 注：standalone MockMvc 无 Security 过滤器链，security 的 authentication() 后置处理器
 * 只写 TestSecurityContextHolder 不写 request.userPrincipal，故这里直接给 mock request
 * 设置 userPrincipal（PrincipalMethodArgumentResolver 从 request 解析 Authentication 参数）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("个人中心控制器测试")
class ProfileControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ProfileController profileController;

    @Mock
    private UserApplicationService userApplicationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("获取当前用户信息 - 返回 200 与用户数据")
    void getCurrentUser_returnsInfo() throws Exception {
        when(userApplicationService.getCurrentUser(1L))
                .thenReturn(Map.of("id", 1L, "nickname", "测试用户"));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(request -> {
                            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(1L, null));
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    @Test
    @DisplayName("修改密码 - 返回 200")
    void changePassword_success() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new12345\"}")
                        .with(request -> {
                            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(1L, null));
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("更新个人资料 - 返回 200 并透传参数")
    void updateProfile_success() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\",\"bio\":\"新的签名\"}")
                        .with(request -> {
                            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(1L, null));
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userApplicationService).updateProfile(eq(1L), bodyCaptor.capture());
        assertEquals("新昵称", bodyCaptor.getValue().get("nickname"));
        assertEquals("新的签名", bodyCaptor.getValue().get("bio"));
    }

    @Test
    @DisplayName("获取当前用户 - 用户不存在返回 404")
    void getCurrentUser_userNotFound_returns404() throws Exception {
        when(userApplicationService.getCurrentUser(1L))
                .thenThrow(new CulturalApiException(404, "用户不存在"));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(request -> {
                            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(1L, null));
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
}
