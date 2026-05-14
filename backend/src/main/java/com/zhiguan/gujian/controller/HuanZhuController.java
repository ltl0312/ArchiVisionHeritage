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

import java.util.Map;

/**
 * 一键幻筑 — AI 3D模型生成
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class HuanZhuController {

    private final TaskOrchestrationService taskOrchestrationService;

    /** 提交幻筑任务，立即返回 HTTP 202 + taskId */
    @PostMapping("/huanzhu")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<Map<String, Object>> submitHuanZhu(@Valid @RequestBody HuanZhuRequest request,
                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long taskId = taskOrchestrationService.submitHuanZhuTask(userId, request.getPrompt());
        return Result.ok(Map.of("taskId", taskId, "status", "PENDING"));
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
