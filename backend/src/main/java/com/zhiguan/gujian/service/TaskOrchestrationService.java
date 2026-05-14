package com.zhiguan.gujian.service;

import com.zhiguan.gujian.dto.response.TaskStatusResponse;

import java.util.List;

public interface TaskOrchestrationService {

    /** 提交幻筑任务，返回 taskId */
    Long submitHuanZhuTask(Long userId, String prompt);

    /** 查询任务状态（前端轮询） */
    TaskStatusResponse getTaskStatus(Long taskId);

    /** 查询用户通知列表 */
    List<?> getNotifications(Long userId);
}
