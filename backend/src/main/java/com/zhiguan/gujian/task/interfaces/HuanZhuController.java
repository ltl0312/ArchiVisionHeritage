package com.zhiguan.gujian.task.interfaces;

import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.task.domain.TaskStatus;
import com.zhiguan.gujian.task.interfaces.HuanZhuRequest;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.shared.security.CallerIdentity;
import com.zhiguan.gujian.task.interfaces.TaskStatusResponse;
import com.zhiguan.gujian.task.application.TaskOrchestrationService;
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
        data.put("status", TaskStatus.PENDING);
        data.put("duplicate", result.duplicate());
        return Result.ok(data);
    }

    /**
     * 轮询任务状态。
     * 必须携带身份：服务层会校验任务归属（非管理员只能查自己的任务），
     * 否则任何登录用户枚举 id 即可读取他人任务的资产路径（IDOR）。
     */
    @GetMapping("/{id}/status")
    public Result<TaskStatusResponse> getTaskStatus(@PathVariable Long id, Authentication auth) {
        TaskStatusResponse status = taskOrchestrationService.getTaskStatus(
                id, CallerIdentity.userId(auth), CallerIdentity.isAdmin(auth));
        if (status == null) {
            throw new CulturalApiException(404, "任务不存在");
        }
        return Result.ok(status);
    }
}
