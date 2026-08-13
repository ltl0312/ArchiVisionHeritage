package com.zhiguan.gujian.auth.application;

import com.zhiguan.gujian.auth.interfaces.LoginRequest;
import com.zhiguan.gujian.auth.interfaces.RegisterRequest;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$10$encodedPassword");
        testUser.setNickname("测试用户");
        testUser.setRole("USER");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("test123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("new123");
        registerRequest.setNickname("新用户");
    }

    @Test
    @DisplayName("登录成功 - 返回 JWT Token")
    void login_success_returnsToken() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("test123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser", "USER")).thenReturn("jwt.token.here");

        String token = authService.login(loginRequest);

        assertNotNull(token);
        assertEquals("jwt.token.here", token);
        verify(userMapper).findByUsername("testuser");
        verify(passwordEncoder).matches("test123", "$2a$10$encodedPassword");
        verify(jwtUtil).generateToken(1L, "testuser", "USER");
    }

    @Test
    @DisplayName("登录失败 - 用户名不存在")
    void login_userNotFound_throwsException() {
        when(userMapper.findByUsername("nonexistent")).thenReturn(null);

        loginRequest.setUsername("nonexistent");

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(401, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_wrongPassword_throwsException() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encodedPassword")).thenReturn(false);

        loginRequest.setPassword("wrongpassword");

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(401, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("注册成功 - 创建新用户")
    void register_success_createsUser() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("new123")).thenReturn("$2a$10$encodedNewPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.register(registerRequest));

        verify(userMapper).findByUsername("newuser");
        verify(passwordEncoder).encode("new123");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_usernameExists_throwsException() {
        when(userMapper.findByUsername("newuser")).thenReturn(testUser);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals(400, exception.getCode());
        assertEquals("用户名已被注册", exception.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册 - 新用户默认角色为 USER")
    void register_defaultRoleIsUser() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("new123")).thenReturn("$2a$10$encodedNewPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        authService.register(registerRequest);

        verify(userMapper).insert(argThat(user -> "USER".equals(user.getRole())));
    }
}
