package com.zhiguan.gujian.aop.aspect;

import com.zhiguan.gujian.annotation.RateLimit;
import com.zhiguan.gujian.exception.CulturalApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * VGGT 分布式限流切面 — 基于 Redis + Lua 原子脚本
 *
 * 替代原 JVM ConcurrentHashMap 方案，多实例部署时计数一致，
 * 次日 TTL 自动清零，无需手动维护日期翻篇逻辑。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    private static final String KEY_PREFIX = "rate:zhixi";

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取当前用户 ID（未登录用户以 IP 区分）
        String userIdentifier = getCurrentUserIdentifier();
        String today = LocalDate.now().toString();
        String key = KEY_PREFIX + ":" + userIdentifier + ":" + today;

        long ttlSeconds = ChronoUnit.SECONDS.between(
                java.time.LocalDateTime.now(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );

        Long allowed = redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(rateLimit.maxCalls()),
                String.valueOf(ttlSeconds)
        );

        if (allowed == null || allowed == 0L) {
            log.info("限流拦截 — 用户 {} 今日 VGGT 调用次数已用完", userIdentifier);
            throw new CulturalApiException(
                    "古建高精几何解析犹如匠人雕琢，需耗费大量云端算力。" +
                    "出于对资源的敬畏与合理配置，平台对单用户实行每日最多 " +
                    rateLimit.maxCalls() + " 次的解析节制。今日额度已用完，请明日再试。");
        }

        return joinPoint.proceed();
    }

    /** 获取用户标识：优先 JWT userId，未登录回退客户端 IP */
    private String getCurrentUserIdentifier() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                return "uid:" + auth.getPrincipal();
            }
        } catch (Exception ignored) {
            // 未登录或 token 无效，使用 fallback
        }

        // 获取客户端 IP 地址
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                // 多个代理时取第一个
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return "ip:" + ip;
            }
        } catch (Exception ignored) {
            // 获取 IP 失败
        }

        return "anonymous";
    }
}
