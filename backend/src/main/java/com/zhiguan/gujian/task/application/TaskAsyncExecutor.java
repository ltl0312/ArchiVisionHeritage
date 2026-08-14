package com.zhiguan.gujian.task.application;

import com.zhiguan.gujian.task.domain.AiTask;
import com.zhiguan.gujian.task.infrastructure.AiTaskMapper;
import com.zhiguan.gujian.task.infrastructure.IdempotentLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 幻筑任务异步编排壳 — @Async 线程入口
 *
 * 职责收敛：任务存在性检查 + 幂等锁生命周期 + 失败兜底；
 * 实际执行（单事务：RUNNING → 模拟生成 → 资产落库 + SUCCESS）下沉至 TaskExecutionService（P0-2 修复）。
 * 通知创建不再在此直插 —— 由 TaskCompletedEventListener 消费完成事件后写入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAsyncExecutor {

    private final AiTaskMapper aiTaskMapper;
    private final IdempotentLockService idempotentLockService;
    private final TaskExecutionService taskExecutionService;

    /**
     * 异步执行 AI 3D 模型生成 — 状态机编排
     *
     * PENDING → TaskExecutionService.execute()（单事务：RUNNING → 模拟生成 → SUCCESS + 资产）
     * 执行异常 → markFailed()（事务已回滚，单条 UPDATE 兜底）
     * 无论成败（含任务不存在）→ 释放幂等锁，用户可再次提交相同描述词
     */
    @Async("taskExecutor")
    public void executeAsync(Long taskId, String originalPrompt, Long userId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("幻筑任务 {} 不存在，跳过执行", taskId);
            releaseLock(originalPrompt, userId);
            return;
        }

        try {
            taskExecutionService.execute(taskId);
        } catch (Exception e) {
            log.error("幻筑任务 {} 营造失败", taskId, e);
            taskExecutionService.markFailed(taskId);
        } finally {
            releaseLock(originalPrompt, userId);
        }
    }

    private void releaseLock(String originalPrompt, Long userId) {
        if (originalPrompt != null && userId != null) {
            idempotentLockService.release(userId, originalPrompt);
            log.debug("幻筑任务幂等锁已释放");
        }
    }
}
