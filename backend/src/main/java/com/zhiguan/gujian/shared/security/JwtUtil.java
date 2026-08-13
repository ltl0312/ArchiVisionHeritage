package com.zhiguan.gujian.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具 — 密钥与过期时间由配置注入（zhiguan.jwt.secret / zhiguan.jwt.expiration），
 * 生产环境可用 JWT_SECRET 环境变量覆盖；无参构造保留默认值供单元测试使用。
 */
@Component
public class JwtUtil {

    public static final String DEFAULT_SECRET = "ZhiGuan-GuJian-2024-SecretKey-For-JWT-Token-Generation-Must-Be-Long-Enough";
    public static final long DEFAULT_EXPIRATION = 86400000L; // 24小时

    private final SecretKey key;
    private final long expiration;

    /** 无参构造 — 供单元测试使用默认密钥/过期时间（与原实现行为一致） */
    public JwtUtil() {
        this(DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    /** Spring 注入构造 — 密钥与过期时间来自配置（zhiguan.jwt.*） */
    @Autowired
    public JwtUtil(@Value("${zhiguan.jwt.secret}") String secret,
                   @Value("${zhiguan.jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /** 签发 JWT — 将 userId、username、role 写入载荷 */
    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /** 从 Token 中提取用户角色，用于 Spring Security 权限校验 */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
