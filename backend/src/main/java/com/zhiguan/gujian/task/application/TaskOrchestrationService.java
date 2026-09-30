package com.zhiguan.gujian.task.application;

import com.zhiguan.gujian.task.interfaces.TaskStatusResponse;

public interface TaskOrchestrationService {

    /**
     * 提交幻筑任务（含幂等守护）。
     *
     * @return SubmitResult — 包含 taskId 和是否为重复提交标识
     */
    SubmitResult submitHuanZhuTask(Long userId, String prompt);

    /**
     * 查询任务状态（前端轮询）。
     *
     * ⚠️ 必须做归属校验：该端点原先只按 taskId 查询，任何登录用户枚举 id
     * 就能读到他人任务的 `preview2dPath` / `glb3dPath` / `errorMessage`（IDOR，已实测）。
     *
     * @param taskId      任务 id
     * @param requesterId 调用方 userId
     * @param isAdmin     调用方是否管理员
     * @return 任务状态；**任务不存在或不属于调用方时返回 null**（由控制器统一转 404，
     *         从而不暴露"这个 id 存在但不属于你"）
     */
    TaskStatusResponse getTaskStatus(Long taskId, Long requesterId, boolean isAdmin);

    /** 提交结果 DTO */
    record SubmitResult(Long taskId, boolean duplicate) {}
}
