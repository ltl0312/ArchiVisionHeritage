package com.zhiguan.gujian.task.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

/**
 * 幂等性守护服务 — 基于 Redis + Lua 原子脚本
 *
 * 场景：AI 3D 幻筑接口 / 高耗时推理任务
 * 策略：以 userId + normalizedPromptHash 为去重维度，
 *       在 Redis 中原子地检查-加锁，彻底阻断重复提交
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> idempotentLockScript;

    /** 幂等锁 TTL：10 分钟（足够一个任务完成或失败后释放） */
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private static final String KEY_PREFIX = "huanzhu:dedup";

    /**
     * 尝试获取幂等锁。
     *
     * @param userId 用户 ID
     * @param prompt 原始 Prompt 文本
     * @return true = 允许创建新任务，false = 检测到重复提交
     */
    public boolean tryAcquire(Long userId, String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        String promptHash = sha256(normalizedPrompt);

        String lockKey = KEY_PREFIX + ":" + userId + ":" + promptHash;
        String taskIdKey = lockKey + ":taskId";

        // 注意：脚本参数经 GenericJackson2JsonRedisSerializer 序列化，
        // 传数字（不带引号）Lua 端 tonumber(ARGV[1]) 才能正确解析
        Long result = redisTemplate.execute(
                idempotentLockScript,
                List.of(lockKey, taskIdKey),
                LOCK_TTL.getSeconds()
        );

        boolean allowed = result != null && result == 1L;
        if (!allowed) {
            log.info("幂等拦截 — 用户 {} 的 Prompt 已有进行中任务 (hash={})", userId, promptHash);
        } else {
            log.debug("幂等放行 — 用户 {} 创建新任务 (hash={})", userId, promptHash);
        }
        return allowed;
    }

    /**
     * 任务创建成功后回填 taskId 至 Redis，使后续重复请求可返回已有任务
     */
    public void updateLockWithTaskId(Long userId, String prompt, Long taskId) {
        String normalizedPrompt = normalizePrompt(prompt);
        String promptHash = sha256(normalizedPrompt);

        String lockKey = KEY_PREFIX + ":" + userId + ":" + promptHash;
        String taskIdKey = lockKey + ":taskId";

        redisTemplate.opsForValue().set(taskIdKey, taskId, LOCK_TTL);
        log.debug("幂等锁已绑定 taskId={} (key={})", taskId, taskIdKey);
    }

    /**
     * 查询已有任务 ID（用于重复提交时直接返回）
     */
    public Long getExistingTaskId(Long userId, String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        String promptHash = sha256(normalizedPrompt);

        String taskIdKey = KEY_PREFIX + ":" + userId + ":" + promptHash + ":taskId";
        Object value = redisTemplate.opsForValue().get(taskIdKey);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 任务完成/失败后释放幂等锁（让用户可再次提交相同描述词）
     */
    public void release(Long userId, String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        String promptHash = sha256(normalizedPrompt);

        String lockKey = KEY_PREFIX + ":" + userId + ":" + promptHash;
        String taskIdKey = lockKey + ":taskId";

        redisTemplate.delete(List.of(lockKey, taskIdKey));
        log.debug("幂等锁已释放 (user={}, hash={})", userId, promptHash);
    }

    /** 规范化 Prompt：去首尾空白、统一小写、压缩多余空格 */
    private String normalizePrompt(String prompt) {
        return prompt.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /** SHA-256 哈希（确定性去重，避免 Prompt 原文过长） */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
