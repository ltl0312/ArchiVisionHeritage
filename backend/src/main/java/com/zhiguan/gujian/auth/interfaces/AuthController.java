package com.zhiguan.gujian.auth.interfaces;

import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.shared.aop.RateLimit;
import com.zhiguan.gujian.auth.interfaces.LoginRequest;
import com.zhiguan.gujian.auth.interfaces.RegisterRequest;
import com.zhiguan.gujian.auth.application.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录。
     *
     * 限流说明：原先登录接口**没有任何限流**，可被无限次暴力尝试。
     * 这里复用现有的 Redis + Lua 每日计数（key 与智析接口区分，不共用计数器）。
     * 注意：现有基础设施是**按日**计数，因此这是"每日 20 次/来源"的粗粒度防护，
     * 不是按分钟滑窗 —— 对暴力破解是有效威慑，但更细的窗口需要另加基础设施。
     */
    @PostMapping("/login")
    @RateLimit(maxCalls = 20, key = "login",
            message = "登录尝试次数过多，为保护账号安全已触发每日限额。请明日再试，或联系管理员。")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return Result.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok();
    }
}
