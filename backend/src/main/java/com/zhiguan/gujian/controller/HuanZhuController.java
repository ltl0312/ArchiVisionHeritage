package com.zhiguan.gujian.controller;

import com.zhiguan.gujian.config.Result;
import com.zhiguan.gujian.dto.request.HuanZhuRequest;
import com.zhiguan.gujian.dto.response.TaskStatusResponse;
import com.zhiguan.gujian.service.TaskOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 一键幻筑 — AI 3D模型生成
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class HuanZhuController {

    private final TaskOrchestrationService taskOrchestrationService;

    /**
     * 提交幻筑任务 — 含 Redis 幂等守护
     * 重复提交相同 Prompt 时返回已有 taskId + duplicate=true，避免双重扣费
     */
    @PostMapping("/huanzhu")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<Map<String, Object>> submitHuanZhu(@Valid @RequestBody HuanZhuRequest request,
                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        TaskOrchestrationService.SubmitResult result =
                taskOrchestrationService.submitHuanZhuTask(userId, request.getPrompt());

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", result.taskId());
        data.put("status", "PENDING");
        data.put("duplicate", result.duplicate());
        return Result.ok(data);
    }

    /** 轮询任务状态 */
    @GetMapping("/{id}/status")
    public Result<TaskStatusResponse> getTaskStatus(@PathVariable Long id) {
        TaskStatusResponse status = taskOrchestrationService.getTaskStatus(id);
        if (status == null) {
            return Result.fail(404, "任务不存在");
        }
        return Result.ok(status);
    }
}
