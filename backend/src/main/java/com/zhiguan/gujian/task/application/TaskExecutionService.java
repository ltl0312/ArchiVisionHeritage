package com.zhiguan.gujian.task.application;

import com.zhiguan.gujian.task.domain.AiTask;
import com.zhiguan.gujian.task.domain.ModelAsset;
import com.zhiguan.gujian.task.domain.TaskStatus;
import com.zhiguan.gujian.task.domain.event.TaskCompletedEvent;
import com.zhiguan.gujian.task.infrastructure.AiTaskMapper;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 幻筑任务执行服务 — 单事务执行（P0-2 修复）
 *
 * RUNNING 更新 + model_asset 插入 + SUCCESS 更新在同一事务内提交；
 * 任一步失败整体回滚，杜绝「asset 已插入但任务 FAILED」的不一致。
 * 成功后发布 TaskCompletedEvent，由通知 BC 监听器在事务提交后（AFTER_COMMIT）创建站内信，
 * 回滚时事件被丢弃、通知不产生，与资产回滚保持一致。
 *
 * 重要：必须在其他 Bean 中调用（TaskAsyncExecutor 跨 Bean 调用才能命中 @Transactional 代理）。
 * 模拟耗时与资产目录经 zhiguan.task.* 外置（P2-8），字段风格与 AnalysisService/WebMvcConfig 一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    /** 失败提示文案（与历史版本一致） */
    public static final String FAIL_ERROR_MESSAGE = "任务执行失败，请稍后重试";

    private final AiTaskMapper aiTaskMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    /** 模拟 AI 3D 生成耗时下限（毫秒） */
    @Value("${zhiguan.task.simulate-min-ms:3000}")
    private long simulateMinMs;

    /** 模拟 AI 3D 生成耗时上限（毫秒） */
    @Value("${zhiguan.task.simulate-max-ms:7000}")
    private long simulateMaxMs;

    /** 2D 封面图目录（文件名 huanzhu_{taskId}_preview.png） */
    @Value("${zhiguan.task.asset-preview-dir:/assets/preview}")
    private String assetPreviewDir;

    /** 3D GLB 模型目录（文件名 huanzhu_{taskId}_model.glb） */
    @Value("${zhiguan.task.asset-model-dir:/assets/models}")
    private String assetModelDir;

    /**
     * 单事务执行幻筑任务：RUNNING 更新 → 模拟生成 → 资产落库 + SUCCESS 更新 → 发布完成事件。
     * 任一异常整体回滚（InterruptedException 已转为 RuntimeException，确保默认回滚策略生效）。
     */
    @Transactional
    public void execute(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            // 防御性检查：afterCommit 保证提交后才执行，正常路径不可达
            throw new IllegalStateException("任务不存在: " + taskId);
        }

        // --- PENDING → RUNNING（营造中）---
        task.setStatus(TaskStatus.RUNNING);
        aiTaskMapper.updateById(task);
        log.info("幻筑任务 {} 状态更新为 RUNNING，营造中...", taskId);

        // 模拟 AI 3D 模型生成耗时（实际接入 Meshy 等 API 时应替换为远端调用）
        simulate();

        // --- RUNNING → SUCCESS：资产 + 状态同事务提交 ---
        ModelAsset asset = new ModelAsset();
        asset.setTaskId(taskId);
        asset.setPreview2dPath(assetPreviewDir + "/huanzhu_" + taskId + "_preview.png");
        asset.setGlb3dPath(assetModelDir + "/huanzhu_" + taskId + "_model.glb");
        modelAssetMapper.insert(asset);

        task.setStatus(TaskStatus.SUCCESS);
        aiTaskMapper.updateById(task);

        // 发布完成事件 — 事务提交后才被 AFTER_COMMIT 监听器消费；回滚则事件不触发
        applicationEventPublisher.publishEvent(new TaskCompletedEvent(taskId, task.getUserId()));

        log.info("幻筑任务 {} 营造成功，资产ID: {}, 封面: {}, 3D模型: {}",
                taskId, asset.getId(), asset.getPreview2dPath(), asset.getGlb3dPath());
    }

    /**
     * 标记任务失败 — 供 TaskAsyncExecutor 在 execute() 事务已回滚后兜底调用。
     * 无需 @Transactional：异常越过代理边界时事务已完成回滚，此处为无事务上下文的单条 UPDATE，自动提交天然原子。
     */
    public void markFailed(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(FAIL_ERROR_MESSAGE);
        aiTaskMapper.updateById(task);
        log.warn("幻筑任务 {} 已标记为 FAILED", taskId);
    }

    /** 模拟 AI 3D 生成耗时（3~7 秒，经 zhiguan.task.simulate-* 配置） */
    private void simulate() {
        long span = Math.max(0L, simulateMaxMs - simulateMinMs);
        long delay = simulateMinMs + (long) (Math.random() * span);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // 转为非受检异常：保证 @Transactional 默认回滚策略生效（受检异常不会触发回滚）
            Thread.currentThread().interrupt();
            throw new RuntimeException("模拟 AI 3D 生成被中断", e);
        }
    }
}
