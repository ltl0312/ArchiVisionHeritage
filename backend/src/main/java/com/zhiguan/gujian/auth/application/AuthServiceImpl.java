package com.zhiguan.gujian.auth.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiguan.gujian.auth.interfaces.LoginRequest;
import com.zhiguan.gujian.auth.interfaces.RegisterRequest;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.shared.security.JwtUtil;
import com.zhiguan.gujian.auth.application.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public String login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new CulturalApiException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CulturalApiException(401, "用户名或密码错误");
        }
        // JWT 中携带 role，供 Security 过滤器提取权限
        String role = user.getRole() != null ? user.getRole() : "USER";
        return jwtUtil.generateToken(user.getId(), user.getUsername(), role);
    }

    @Override
    public void register(RegisterRequest request) {
        User existing = userMapper.findByUsername(request.getUsername());
        if (existing != null) {
            throw new CulturalApiException(400, "用户名已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");  // 新注册用户默认为普通用户
        userMapper.insert(user);
    }
}
