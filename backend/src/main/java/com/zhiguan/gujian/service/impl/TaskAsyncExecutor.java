package com.zhiguan.gujian.service.impl;

import com.zhiguan.gujian.mapper.AiTaskMapper;
import com.zhiguan.gujian.mapper.ModelAssetMapper;
import com.zhiguan.gujian.mapper.NotificationMapper;
import com.zhiguan.gujian.model.AiTask;
import com.zhiguan.gujian.model.ModelAsset;
import com.zhiguan.gujian.model.Notification;
import com.zhiguan.gujian.service.IdempotentLockService;
import com.zhiguan.gujian.utils.AncientDictUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 幻筑任务异步执行器 — 独立类解决 @Async 自调用失效问题
 *
 * Spring 的 @Async 基于 AOP 代理实现，同类内部方法调用不会经过代理，
 * 因此将异步执行逻辑抽取到独立的 Bean 中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAsyncExecutor {

    private final AiTaskMapper aiTaskMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final NotificationMapper notificationMapper;
    private final IdempotentLockService idempotentLockService;

    /**
     * 异步执行 AI 3D 模型生成 — 状态机核心
     *
     * PENDING → RUNNING：任务开始营造
     *   ↓
     * 模拟远端 AI 3D 生成（实际接入时应使用 RestTemplate 调用 Meshy 等 API）
     *   ↓
     * SUCCESS：写入 model_asset + notification 站内信 + 释放幂等锁
     * FAILED：记录异常信息 + 释放幂等锁
     */
    @Async("taskExecutor")
    public void executeAsync(Long taskId, String originalPrompt, Long userId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("幻筑任务 {} 不存在，跳过执行", taskId);
            // 释放幂等锁
            if (originalPrompt != null && userId != null) {
                idempotentLockService.release(userId, originalPrompt);
            }
            return;
        }

        try {
            // --- PENDING → RUNNING ---
            task.setStatus("RUNNING");
            aiTaskMapper.updateById(task);
            log.info("幻筑任务 {} 状态更新为 RUNNING，营造中...", taskId);

            // 模拟 AI 3D 模型生成耗时（实际接入 Meshy API 时应使用 RestTemplate 远端调用）
            Thread.sleep(3000 + (long) (Math.random() * 4000));

            // --- RUNNING → SUCCESS ---
            ModelAsset asset = new ModelAsset();
            asset.setTaskId(taskId);
            asset.setPreview2dPath("/assets/preview/huanzhu_" + taskId + "_preview.png");
            asset.setGlb3dPath("/assets/models/huanzhu_" + taskId + "_model.glb");
            modelAssetMapper.insert(asset);

            task.setStatus("SUCCESS");
            aiTaskMapper.updateById(task);

            // 写入站内信通知 — "数字锦盒已送达"
            Notification notification = new Notification();
            notification.setUserId(task.getUserId());
            notification.setTaskId(taskId);
            notification.setMessage("您的古建数字锦盒已送达，请拆阅");
            notification.setIsRead(false);
            notificationMapper.insert(notification);

            log.info("幻筑任务 {} 营造成功，资产ID: {}, 封面: {}, 3D模型: {}",
                    taskId, asset.getId(), asset.getPreview2dPath(), asset.getGlb3dPath());

        } catch (Exception e) {
            log.error("幻筑任务 {} 营造失败", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage("任务执行失败，请稍后重试");
            aiTaskMapper.updateById(task);
        } finally {
            // 无论成败，释放幂等锁（用户可再次提交相同描述词）
            if (originalPrompt != null && userId != null) {
                idempotentLockService.release(userId, originalPrompt);
                log.debug("幻筑任务 {} 幂等锁已释放", taskId);
            }
        }
    }
}
