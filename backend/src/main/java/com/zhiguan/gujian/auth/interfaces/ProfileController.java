package com.zhiguan.gujian.auth.interfaces;

import com.zhiguan.gujian.auth.application.UserApplicationService;
import com.zhiguan.gujian.shared.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 个人中心 — 原 CommunityController 的 profile 端点（URL/JSON 契约不变）
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final UserApplicationService userApplicationService;

    /** 获取当前登录用户完整信息 */
    @GetMapping("/api/v1/users/me")
    public Result<Map<String, Object>> getCurrentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(userApplicationService.getCurrentUser(userId));
    }

    /** 更新当前用户个人资料（昵称、头像、文化签名） */
    @PutMapping("/api/v1/users/me")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        userApplicationService.updateProfile(userId, body);
        return Result.ok();
    }

    /** 修改密码 */
    @PutMapping("/api/v1/users/me/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        userApplicationService.changePassword(userId, body);
        return Result.ok();
    }
}
