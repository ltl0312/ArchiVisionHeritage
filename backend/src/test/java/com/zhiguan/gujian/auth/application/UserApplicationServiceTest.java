package com.zhiguan.gujian.auth.application;

import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * UserApplicationService 单元测试 — 个人中心逻辑（自 CommunityController 迁出）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("个人中心应用服务测试")
class UserApplicationServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserApplicationService userApplicationService;

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("获取当前用户 - 成功返回完整信息")
    void getCurrentUser_success() {
        when(userMapper.selectById(1L)).thenReturn(buildUser());

        Map<String, Object> result = userApplicationService.getCurrentUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("id"));
        assertEquals("测试用户", result.get("nickname"));
        assertEquals("", result.get("avatarUrl"));   // null 安全兜底
        assertEquals("USER", result.get("role"));
    }

    @Test
    @DisplayName("获取当前用户 - 用户不存在抛出 404")
    void getCurrentUser_userNotFound_throws404() {
        when(userMapper.selectById(999L)).thenReturn(null);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> userApplicationService.getCurrentUser(999L)
        );

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 原密码错误抛出 400")
    void changePassword_wrongOldPassword_throws400() {
        User user = buildUser();
        user.setPasswordHash("$2a$12$oldhash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$12$oldhash")).thenReturn(false);

        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "wrong");
        body.put("newPassword", "newpass123");

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> userApplicationService.changePassword(1L, body)
        );

        assertEquals(400, exception.getCode());
        assertEquals("原密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 成功更新为编码后的新密码")
    void changePassword_success() {
        User user = buildUser();
        user.setPasswordHash("$2a$12$oldhash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("oldpass", "$2a$12$oldhash")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$12$newhash");

        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "oldpass");
        body.put("newPassword", "newpass123");

        assertDoesNotThrow(() -> userApplicationService.changePassword(1L, body));

        verify(userMapper).updateById(argThat(u -> "$2a$12$newhash".equals(u.getPasswordHash())));
    }
}
