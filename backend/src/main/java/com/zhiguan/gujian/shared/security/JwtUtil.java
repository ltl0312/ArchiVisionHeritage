package com.zhiguan.gujian.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具 — 密钥与过期时间由配置注入（zhiguan.jwt.secret / zhiguan.jwt.expiration）。
 *
 * 安全约束：
 *   1. 密钥**必须**来自环境变量，配置里不得留默认值（否则公开仓库即等于公开密钥）；
 *   2. 密钥长度不足 32 字节（HS256 的 256 位要求）直接启动失败，并给出可执行的修复提示；
 *   3. 若检测到密钥仍是历史上写入源码的那一个，打印醒目告警 —— 防止有人把默认值加回来。
 *
 * 无参构造保留 {@link #DEFAULT_SECRET}，仅供单元测试构造实例使用。
 */
@Slf4j
@Component
public class JwtUtil {

    /** 历史上写入源码/配置的默认密钥。**任何运行环境都不应再使用它。** */
    public static final String DEFAULT_SECRET = "ZhiGuan-GuJian-2024-SecretKey-For-JWT-Token-Generation-Must-Be-Long-Enough";
    public static final long DEFAULT_EXPIRATION = 86400000L; // 24小时

    /** HS256 要求密钥至少 256 位 */
    private static final int MIN_SECRET_BYTES = 32;

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
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "zhiguan.jwt.secret 至少需要 " + MIN_SECRET_BYTES + " 字节（HS256 要求 256 位）。"
                            + "请设置环境变量 JWT_SECRET，生成方式：openssl rand -hex 48");
        }
        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("⚠️ JWT 密钥仍是写入仓库的默认值 —— 任何读过本仓库的人都能伪造令牌。"
                    + "请用环境变量 JWT_SECRET 覆盖（openssl rand -hex 48）。");
        }
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
