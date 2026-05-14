package com.zhiguan.gujian.service;

import com.zhiguan.gujian.dto.response.TaskStatusResponse;

import java.util.List;

public interface TaskOrchestrationService {

    /**
     * 提交幻筑任务（含幂等守护）。
     *
     * @return SubmitResult — 包含 taskId 和是否为重复提交标识
     */
    SubmitResult submitHuanZhuTask(Long userId, String prompt);

    /** 查询任务状态（前端轮询） */
    TaskStatusResponse getTaskStatus(Long taskId);

    /** 查询用户通知列表 */
    List<?> getNotifications(Long userId);

    /** 提交结果 DTO */
    record SubmitResult(Long taskId, boolean duplicate) {}
}
