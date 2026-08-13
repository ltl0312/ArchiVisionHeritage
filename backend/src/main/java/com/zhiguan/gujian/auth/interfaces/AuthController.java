package com.zhiguan.gujian.auth.interfaces;

import com.zhiguan.gujian.shared.common.Result;
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

    @PostMapping("/login")
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
