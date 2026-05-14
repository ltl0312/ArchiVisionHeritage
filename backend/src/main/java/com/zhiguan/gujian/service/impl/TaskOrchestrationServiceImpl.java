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
import com.zhiguan.gujian.utils.AncientDictUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 一键幻筑核心调度服务：基于 @Async 实现后台异步状态机流转
 *
 * 状态流转：PENDING (排队) → RUNNING (营造中) → SUCCESS (成功) / FAILED (失败)
 *
 * 模块交互机制：
 *   1. HuanZhuController 接收用户 Prompt → submitHuanZhuTask() 存入 ai_task 表（状态 PENDING）
 *   2. 异步线程 executeAsync() 启动：
 *      a. 调用 AncientDictUtil.enhance() 对 Prompt 进行文化降维增强
 *      b. 状态更新为 RUNNING，模拟调用远端 AI 3D 生成 API
 *      c. 生成成功后：
 *         - 将 2D 封面图与 3D GLB 文件路径写入 model_asset 表
 *         - 更新 ai_task 状态为 SUCCESS
 *         - 向 notification 表写入站内信："您的古建数字锦盒已送达，请拆阅"
 *      d. 生成失败：更新状态为 FAILED 并记录异常信息
 *   3. 前端通过 GET /api/v1/tasks/{id}/status 轮询任务状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOrchestrationServiceImpl implements TaskOrchestrationService {

    private final AiTaskMapper aiTaskMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    /**
     * 提交幻筑任务 — 立即将 PENDING 状态写入数据库并返回 taskId
     * 调用 AncientDictUtil 进行文化增强后存储，异步线程接管后续生成流程
     */
    @Override
    @Transactional
    public Long submitHuanZhuTask(Long userId, String prompt) {
        // 使用 AncientDictUtil 进行文化词库增强
        String enhancedPrompt = AncientDictUtil.enhance(prompt);

        AiTask task = new AiTask();
        task.setUserId(userId);
        task.setTaskType("HUANZHU_3D");
        task.setOriginalPrompt(prompt);
        task.setEnhancedPrompt(enhancedPrompt);
        task.setStatus("PENDING");
        aiTaskMapper.insert(task);

        log.info("幻筑任务 {} 已入队，原始Prompt: {} → 增强后: {}", task.getId(), prompt, enhancedPrompt);

        // 提交至异步线程池执行后续生成流程
        executeAsync(task.getId());

        return task.getId();
    }

    /**
     * 异步执行 AI 3D 模型生成 — 状态机核心
     *
     * PENDING → RUNNING：任务开始营造
     *   ↓
     * 模拟远端 AI 3D 生成（实际接入时应使用 RestTemplate 调用 Meshy 等 API）
     *   ↓
     * SUCCESS：写入 model_asset + notification 站内信
     * FAILED：记录异常信息
     */
    @Async("taskExecutor")
    public void executeAsync(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("幻筑任务 {} 不存在，跳过执行", taskId);
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
            // 将生成的 2D 封面图与 3D GLB 文件路径写入 model_asset 表
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
            task.setErrorMessage(e.getMessage());
            aiTaskMapper.updateById(task);
        }
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

        if ("SUCCESS".equals(task.getStatus())) {
            // 查找关联的 3D 资产
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
    public List<?> getNotifications(Long userId) {
        return notificationService.getNotifications(userId);
    }
}
