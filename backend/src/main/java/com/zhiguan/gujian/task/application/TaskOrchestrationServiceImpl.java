package com.zhiguan.gujian.task.application;

import com.zhiguan.gujian.task.interfaces.TaskStatusResponse;
import com.zhiguan.gujian.task.infrastructure.AiTaskMapper;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import com.zhiguan.gujian.task.domain.AiTask;
import com.zhiguan.gujian.task.domain.ModelAsset;
import com.zhiguan.gujian.task.domain.TaskStatus;
import com.zhiguan.gujian.task.infrastructure.IdempotentLockService;
import com.zhiguan.gujian.task.application.TaskOrchestrationService;
import com.zhiguan.gujian.shared.util.AncientDictUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 一键幻筑核心调度服务：基于 @Async 实现后台异步状态机流转
 *
 * 状态流转：PENDING (排队) → RUNNING (营造中) → SUCCESS (成功) / FAILED (失败)
 *
 * 模块交互机制：
 *   1. HuanZhuController 接收用户 Prompt → submitHuanZhuTask()
 *      a. IdempotentLockService 基于 Redis + Lua 原子检查是否重复提交
 *      b. 如重复：直接返回已有 taskId（幂等守护，杜绝双重扣费）
 *      c. 如非重复：ai_task 表写入 PENDING 状态 → Redis 记录 taskId
 *   2. 异步线程 executeAsync() 启动：
 *      a. 调用 AncientDictUtil.enhance() 对 Prompt 进行文化降维增强
 *      b. 状态更新为 RUNNING，模拟调用远端 AI 3D 生成 API
 *      c. 生成成功后：
 *         - 将 2D 封面图与 3D GLB 文件路径写入 model_asset 表
 *         - 更新 ai_task 状态为 SUCCESS
 *         - 向 notification 表写入站内信："您的古建数字锦盒已送达，请拆阅"
 *      d. 生成失败：更新状态为 FAILED 并记录异常信息
 *      e. 无论成败，释放 Redis 幂等锁
 *   3. 前端通过 GET /api/v1/tasks/{id}/status 轮询任务状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOrchestrationServiceImpl implements TaskOrchestrationService {

    private final AiTaskMapper aiTaskMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final IdempotentLockService idempotentLockService;
    private final TaskAsyncExecutor taskAsyncExecutor;

    /**
     * 提交幻筑任务 — Redis 幂等守护 + PENDING 状态写入
     *
     * 幂等策略：以 userId + normalizedPromptHash 为维度的短期锁（TTL 10min），
     * 同一用户提交相同描述词时，Lua 原子脚本拦截重复请求，返回已有 taskId。
     */
    @Override
    @Transactional
    public SubmitResult submitHuanZhuTask(Long userId, String prompt) {
        // 1. Redis Lua 原子幂等检查
        if (!idempotentLockService.tryAcquire(userId, prompt)) {
            Long existingTaskId = idempotentLockService.getExistingTaskId(userId, prompt);
            if (existingTaskId != null) {
                log.info("幂等命中 — 用户 {} 重复提交 Prompt，返回已有任务 {}", userId, existingTaskId);
                return new SubmitResult(existingTaskId, true);
            }
            // 边界情况：锁存在但 taskId 尚未回填（极端并发穿透），放行创建
            log.warn("幂等锁存在但 taskId 缺失 — 用户 {}，降级放行", userId);
        }

        // 2. 文化词库增强
        String enhancedPrompt = AncientDictUtil.enhance(prompt);

        // 3. 写入 ai_task 表
        AiTask task = new AiTask();
        task.setUserId(userId);
        task.setTaskType("HUANZHU_3D");
        task.setOriginalPrompt(prompt);
        task.setEnhancedPrompt(enhancedPrompt);
        task.setStatus(TaskStatus.PENDING);
        aiTaskMapper.insert(task);

        // 4. Redis 回填 taskId（使后续重复请求可直接定位）
        idempotentLockService.updateLockWithTaskId(userId, prompt, task.getId());

        log.info("幻筑任务 {} 已入队，原始Prompt: {} → 增强后: {}", task.getId(), prompt, enhancedPrompt);

        // 5. 事务提交后再触发异步执行（P0-1 修复）：
        //    异步线程必须在 TX 提交后才能读到 ai_task 行，
        //    否则会走「任务不存在」分支释放锁，导致任务永久停留在 PENDING
        Long taskId = task.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskAsyncExecutor.executeAsync(taskId, prompt, userId);
                }
            });
        } else {
            // 无事务上下文（如纯单测环境）兜底：直接提交异步执行
            taskAsyncExecutor.executeAsync(taskId, prompt, userId);
        }

        return new SubmitResult(taskId, false);
    }

    /**
     * 轮询任务状态 — 前端定期调用以获取当前营造进度
     * 当状态为 SUCCESS 时一并返回关联的 model_asset 信息（封面图 + GLB 路径）
     */
    @Override
    public TaskStatusResponse getTaskStatus(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) return null;

        TaskStatusResponse.TaskStatusResponseBuilder builder = TaskStatusResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .errorMessage(task.getErrorMessage());

        if (TaskStatus.SUCCESS.equals(task.getStatus())) {
            ModelAsset asset = modelAssetMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ModelAsset>()
                            .eq(ModelAsset::getTaskId, taskId));
            if (asset != null) {
                builder.assetId(asset.getId())
                       .preview2dPath(asset.getPreview2dPath())
                       .glb3dPath(asset.getGlb3dPath());
            }
        }

        return builder.build();
    }
}
