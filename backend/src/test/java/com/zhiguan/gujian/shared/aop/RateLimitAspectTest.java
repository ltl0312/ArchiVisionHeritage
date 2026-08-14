package com.zhiguan.gujian.shared.aop;

import com.zhiguan.gujian.shared.common.CulturalApiException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitAspect 单元测试 — Redis Lua 限流三分支 + Key/参数契约
 *
 * 直接 mock RedisTemplate（不依赖真实 Redis）：放行(1L) / 超限(0L) / 脚本异常(null)。
 * "次日清零"由 Lua 脚本 EXPIRE 承担，此处断言 TTL 秒数为正（当日剩余时间）等价覆盖。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("限流切面测试")
class RateLimitAspectTest {

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DefaultRedisScript<Long> rateLimitScript;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @RateLimit(maxCalls = 5)
    public void annotatedEndpoint() {
        // 仅用于反射获取 @RateLimit 注解实例
    }

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private RateLimit rateLimitAnnotation() throws Exception {
        return getClass().getMethod("annotatedEndpoint").getAnnotation(RateLimit.class);
    }

    @Test
    @DisplayName("计数未超限(脚本返回1) - 放行并执行目标方法")
    void allowed_proceeds() throws Throwable {
        when(redisTemplate.execute(eq(rateLimitScript), anyList(), any(Object[].class))).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = rateLimitAspect.checkRateLimit(joinPoint, rateLimitAnnotation());

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("今日额度用尽(脚本返回0) - 抛 429 且不执行目标方法")
    void exceeded_throws429AndSkipsTarget() throws Throwable {
        when(redisTemplate.execute(eq(rateLimitScript), anyList(), any(Object[].class))).thenReturn(0L);

        CulturalApiException ex = assertThrows(CulturalApiException.class,
                () -> rateLimitAspect.checkRateLimit(joinPoint, rateLimitAnnotation()));

        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("今日额度已用完"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("脚本异常(返回null) - 视为超限拦截")
    void nullResult_throws() throws Throwable {
        when(redisTemplate.execute(eq(rateLimitScript), anyList(), any(Object[].class))).thenReturn(null);

        assertThrows(CulturalApiException.class,
                () -> rateLimitAspect.checkRateLimit(joinPoint, rateLimitAnnotation()));

        verify(joinPoint, never()).proceed();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("未登录 - Key 用 IP 前缀, maxCalls=5, TTL 为正(次日清零载体)")
    void keyAndArgs_ipIdentifier_ttlPositive() throws Throwable {
        when(redisTemplate.execute(eq(rateLimitScript), anyList(), any(Object[].class))).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        rateLimitAspect.checkRateLimit(joinPoint, rateLimitAnnotation());

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(rateLimitScript), keysCaptor.capture(), argsCaptor.capture());

        assertEquals(List.of("rate:zhixi:ip:127.0.0.1:" + LocalDate.now()), keysCaptor.getValue());
        Object[] args = argsCaptor.getValue();
        assertEquals(5, args[0]);       // maxCalls 透传
        assertTrue((Long) args[1] > 0); // TTL = 距次日零点秒数
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("已登录 - Key 用 uid 前缀(优先 JWT userId)")
    void keyAndArgs_uidIdentifier_whenAuthenticated() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null));
        when(redisTemplate.execute(eq(rateLimitScript), anyList(), any(Object[].class))).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        rateLimitAspect.checkRateLimit(joinPoint, rateLimitAnnotation());

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(eq(rateLimitScript), keysCaptor.capture(), any(Object[].class));

        assertEquals(List.of("rate:zhixi:uid:1:" + LocalDate.now()), keysCaptor.getValue());
    }
}
