package com.zhiguan.gujian.task.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IdempotentLockService 单元测试 — Lua 三分支：放行(1) / 重复(0) / 锁占用(null 保守拒绝)。
 * 另覆盖 Key 结构（huanzhu:dedup:{userId}:{sha256} + :taskId 子键）、TTL=600s、
 * getExistingTaskId 类型判定与 release/updateLock 的 Redis 调用契约。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("幂等锁服务测试")
class IdempotentLockServiceTest {

    @InjectMocks
    private IdempotentLockService idempotentLockService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DefaultRedisScript<Long> idempotentLockScript;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Lua 返回1 - 放行, Key 结构与 TTL=600s 正确")
    void tryAcquire_allowed_returnsTrue() {
        String prompt = "  唐代大殿  ";          // 首尾空白应被 normalize 去除
        String expectedHash = sha256("唐代大殿");
        when(redisTemplate.execute(eq(idempotentLockScript), anyList(), any(Object[].class))).thenReturn(1L);

        assertTrue(idempotentLockService.tryAcquire(1L, prompt));

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(idempotentLockScript), keysCaptor.capture(), argsCaptor.capture());

        List<String> keys = keysCaptor.getValue();
        assertEquals("huanzhu:dedup:1:" + expectedHash, keys.get(0));
        assertEquals(keys.get(0) + ":taskId", keys.get(1));
        assertTrue(HASH_PATTERN.matcher(expectedHash).matches());
        assertEquals(600L, argsCaptor.getValue()[0]);   // LOCK_TTL = 10min
    }

    @Test
    @DisplayName("Lua 返回0(重复/锁占用) - 拒绝创建")
    void tryAcquire_duplicate_returnsFalse() {
        when(redisTemplate.execute(eq(idempotentLockScript), anyList(), any(Object[].class))).thenReturn(0L);

        assertFalse(idempotentLockService.tryAcquire(1L, "唐代大殿"));
    }

    @Test
    @DisplayName("Lua 返回null(脚本异常) - 保守拒绝")
    void tryAcquire_scriptNull_returnsFalse() {
        when(redisTemplate.execute(eq(idempotentLockScript), anyList(), any(Object[].class))).thenReturn(null);

        assertFalse(idempotentLockService.tryAcquire(1L, "唐代大殿"));
    }

    @Test
    @DisplayName("getExistingTaskId - Number 值返回 long")
    void getExistingTaskId_number_returnsLong() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(42L);

        assertEquals(42L, idempotentLockService.getExistingTaskId(1L, "唐代大殿"));
        verify(valueOperations).get("huanzhu:dedup:1:" + sha256("唐代大殿") + ":taskId");
    }

    @Test
    @DisplayName("getExistingTaskId - 字符串值返回 null")
    void getExistingTaskId_string_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("abc");

        assertNull(idempotentLockService.getExistingTaskId(1L, "唐代大殿"));
    }

    @Test
    @DisplayName("updateLockWithTaskId - 回填 taskId 并设置 TTL")
    void updateLockWithTaskId_setsWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        idempotentLockService.updateLockWithTaskId(1L, "唐代大殿", 42L);

        verify(valueOperations).set(
                "huanzhu:dedup:1:" + sha256("唐代大殿") + ":taskId",
                42L,
                Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("release - 同时删除锁 Key 与 taskId 子键")
    void release_deletesBothKeys() {
        idempotentLockService.release(1L, "唐代大殿");

        String lockKey = "huanzhu:dedup:1:" + sha256("唐代大殿");
        verify(redisTemplate).delete(List.of(lockKey, lockKey + ":taskId"));
    }
}
