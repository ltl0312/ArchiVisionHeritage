package com.zhiguan.gujian.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
@DisplayName("JWT 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    @DisplayName("生成 Token 并解析 userId")
    void generateToken_andGetUserId() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");
        assertNotNull(token);
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(1L, userId);
    }

    @Test
    @DisplayName("生成 Token 并解析 username")
    void generateToken_andGetUsername() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");
        String username = jwtUtil.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("生成 Token 并解析 role")
    void generateToken_andGetRole() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
        String role = jwtUtil.getRoleFromToken(token);
        assertEquals("ADMIN", role);
    }

    @Test
    @DisplayName("有效 Token 验证返回 true")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("无效 Token 验证返回 false")
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("空 Token 验证返回 false")
    void validateToken_emptyToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    @DisplayName("null Token 验证返回 false")
    void validateToken_nullToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    @DisplayName("篡改 Token 验证返回 false")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");
        // 篡改 Token 的载荷部分
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("不同用户生成的 Token 不同")
    void generateToken_differentUsers_differentTokens() {
        String token1 = jwtUtil.generateToken(1L, "user1", "USER");
        String token2 = jwtUtil.generateToken(2L, "user2", "USER");
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("解析不存在的字段返回 null")
    void getToken_nonexistentClaim_returnsNull() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");
        // role 字段存在，应该返回
        assertNotNull(jwtUtil.getRoleFromToken(token));
    }
}
