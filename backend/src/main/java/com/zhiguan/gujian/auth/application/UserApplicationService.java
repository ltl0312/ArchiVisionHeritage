package com.zhiguan.gujian.auth.application;

import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 个人中心应用服务 — 从 CommunityController 迁出（消除 Controller 直连 UserMapper/PasswordEncoder，P1-2）
 * URL 契约不变：GET/PUT /api/v1/users/me, PUT /api/v1/users/me/password
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 获取当前登录用户完整信息 */
    public Map<String, Object> getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new CulturalApiException(404, "用户不存在");
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "bio", user.getBio() != null ? user.getBio() : "",
                "role", user.getRole() != null ? user.getRole() : "USER",
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        );
    }

    /** 更新当前用户个人资料（昵称、头像、文化签名） */
    public void updateProfile(Long userId, Map<String, String> body) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new CulturalApiException(404, "用户不存在");

        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        if (body.containsKey("avatarUrl")) user.setAvatarUrl(body.get("avatarUrl"));
        if (body.containsKey("bio")) user.setBio(body.get("bio"));
        userMapper.updateById(user);
    }

    /** 修改密码 */
    public void changePassword(Long userId, Map<String, String> body) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new CulturalApiException(404, "用户不存在");

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            throw new CulturalApiException(400, "密码格式不正确");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new CulturalApiException(400, "原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
