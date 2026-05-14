package com.zhiguan.gujian.service.impl;

import com.zhiguan.gujian.dto.response.TaskStatusResponse;
import com.zhiguan.gujian.mapper.AiTaskMapper;
import com.zhiguan.gujian.mapper.ModelAssetMapper;
import com.zhiguan.gujian.mapper.NotificationMapper;
import com.zhiguan.gujian.model.AiTask;
import com.zhiguan.gujian.model.ModelAsset;
import com.zhiguan.gujian.model.Notification;
import com.zhiguan.gujian.service.TaskOrchestrationService;
import com.zhiguan.gujian.service.NotificationService;
import com.zhiguan.gujian.utils.PromptEnhancerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 核心调度服务：异步处理 PENDING -> RUNNING -> SUCCESS 状态流转
 * 使用 @Async 注解，接收请求后立即返回 202，后台模拟AI生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOrchestrationServiceImpl implements TaskOrchestrationService {

    private final AiTaskMapper aiTaskMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Long submitHuanZhuTask(Long userId, String prompt) {
        // 文化词库增强 Prompt
        String enhancedPrompt = PromptEnhancerUtil.enhance(prompt);

        AiTask task = new AiTask();
        task.setUserId(userId);
        task.setTaskType("HUANZHU_3D");
        task.setOriginalPrompt(prompt);
        task.setEnhancedPrompt(enhancedPrompt);
        task.setStatus("PENDING");
        aiTaskMapper.insert(task);

        // 异步执行生成
        executeAsync(task.getId());

        return task.getId();
    }

    @Async("taskExecutor")
    public void executeAsync(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) return;

        try {
            // 状态更新为 RUNNING
            task.setStatus("RUNNING");
            aiTaskMapper.updateById(task);
            log.info("幻筑任务 {} 开始执行，增强提示词: {}", taskId, task.getEnhancedPrompt());

            // 模拟AI生成耗时（实际接入Meshy等远端API时应使用RestTemplate调用）
            Thread.sleep(3000 + (long)(Math.random() * 4000));

            // 模拟生成资产
            ModelAsset asset = new ModelAsset();
            asset.setTaskId(taskId);
            asset.setPreview2dPath("/assets/preview/mock_preview_" + taskId + ".png");
            asset.setGlb3dPath("/assets/models/mock_model_" + taskId + ".glb");
            modelAssetMapper.insert(asset);

            // 任务成功
            task.setStatus("SUCCESS");
            aiTaskMapper.updateById(task);

            // 发送站内信通知 — "数字锦盒已送达"
            Notification notification = new Notification();
            notification.setUserId(task.getUserId());
            notification.setTaskId(taskId);
            notification.setMessage("您的一键幻筑「" + task.getOriginalPrompt() + "」已完成，数字锦盒已送达！");
            notification.setIsRead(false);
            notificationMapper.insert(notification);

            log.info("幻筑任务 {} 执行成功，资产ID: {}", taskId, asset.getId());

        } catch (Exception e) {
            log.error("幻筑任务 {} 执行失败", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            aiTaskMapper.updateById(task);
        }
    }

    @Override
    public TaskStatusResponse getTaskStatus(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) return null;

        TaskStatusResponse.TaskStatusResponseBuilder builder = TaskStatusResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .errorMessage(task.getErrorMessage());

        if ("SUCCESS".equals(task.getStatus())) {
            // 查找关联资产
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

    @Override
    public java.util.List<?> getNotifications(Long userId) {
        return notificationService.getNotifications(userId);
    }
}
