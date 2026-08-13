package com.zhiguan.gujian.shared.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
// 注：shared/web → community BC 的临时跨 BC 依赖（文档 §5.6 明示，后续演进再细化拆分）
import com.zhiguan.gujian.community.application.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器 — 内容审核与社区治理
 *
 * Spring Security 双重防护：
 *   1. SecurityConfig URL 层面：/api/v1/admin/** → hasRole('ADMIN')
 *   2. 方法注解层面：@PreAuthorize("hasRole('ADMIN')") 防止配置遗漏
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PostService postService;

    /** 获取待审核帖子列表 (status = PENDING) */
    @GetMapping("/posts/pending")
    public Result<Page<PostBriefResponse>> getPendingPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(postService.getPendingPosts(page, size));
    }

    /**
     * 审核帖子 — 通过或驳回
     *
     * @param id     帖子ID
     * @param body   { "status": "APPROVED" | "REJECTED", "rejectReason": "驳回原因(可选)" }
     *               状态机约束：PENDING → APPROVED / REJECTED
     */
    @PutMapping("/posts/{id}/audit")
    public Result<Void> auditPost(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String rejectReason = body.getOrDefault("rejectReason", null);

        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new CulturalApiException(400, "status 必须为 APPROVED 或 REJECTED");
        }

        postService.auditPost(id, status, rejectReason);
        return Result.ok();
    }
}
