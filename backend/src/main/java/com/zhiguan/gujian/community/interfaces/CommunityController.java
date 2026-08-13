package com.zhiguan.gujian.community.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.community.interfaces.CommentRequest;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.CommentResponse;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.community.application.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ======================== 社区帖子流 ========================

    /** 获取帖子流（仅展示 APPROVED 帖子） */
    @GetMapping("/api/v1/posts")
    public Result<Page<PostBriefResponse>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(communityService.getPostFeed(page, size));
    }

    /** 获取帖子详情 */
    @GetMapping("/api/v1/posts/{id}")
    public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, Authentication auth) {
        Long currentUserId = null;
        if (auth != null && auth.getPrincipal() instanceof Long) {
            currentUserId = (Long) auth.getPrincipal();
        }
        return Result.ok(communityService.getPostDetail(id, currentUserId));
    }

    /** 发表帖子 — 默认状态 PENDING，待管理员审核 */
    @PostMapping("/api/v1/posts")
    public Result<Void> createPost(@Valid @RequestBody CreatePostRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        communityService.createPost(userId, request);
        return Result.ok();
    }

    /** 发表评论 */
    @PostMapping("/api/v1/posts/{id}/comments")
    public Result<CommentResponse> addComment(@PathVariable Long id,
                                               @Valid @RequestBody CommentRequest request,
                                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(communityService.addComment(userId, id, request));
    }

    /** 点赞/取消点赞 (防抖) */
    @PostMapping("/api/v1/interactions/like")
    public Result<Void> toggleLike(@Valid @RequestBody LikeRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        communityService.toggleLike(userId, request);
        return Result.ok();
    }

    /** 关注/取消关注 */
    @PostMapping("/api/v1/users/{id}/follow")
    public Result<Void> toggleFollow(@PathVariable Long id, Authentication auth) {
        Long followerId = (Long) auth.getPrincipal();
        communityService.toggleFollow(followerId, id);
        return Result.ok();
    }

    // ======================== 个人中心 (User Profile) ========================

    /** 获取当前登录用户完整信息 */
    @GetMapping("/api/v1/users/me")
    public Result<Map<String, Object>> getCurrentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userMapper.selectById(userId);
        if (user == null) throw new CulturalApiException(404, "用户不存在");
        return Result.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "bio", user.getBio() != null ? user.getBio() : "",
                "role", user.getRole() != null ? user.getRole() : "USER",
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    /** 更新当前用户个人资料（昵称、头像、文化签名） */
    @PutMapping("/api/v1/users/me")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userMapper.selectById(userId);
        if (user == null) throw new CulturalApiException(404, "用户不存在");

        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        if (body.containsKey("avatarUrl")) user.setAvatarUrl(body.get("avatarUrl"));
        if (body.containsKey("bio")) user.setBio(body.get("bio"));
        userMapper.updateById(user);
        return Result.ok();
    }

    /** 修改密码 */
    @PutMapping("/api/v1/users/me/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
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
        return Result.ok();
    }

    /** 获取当前用户发布的帖子 */
    @GetMapping("/api/v1/users/me/posts")
    public Result<Page<PostBriefResponse>> getMyPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(communityService.getUserPosts(userId, page, size));
    }

    /** 获取当前用户点赞过的帖子 */
    @GetMapping("/api/v1/users/me/likes")
    public Result<Page<PostBriefResponse>> getMyLikedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(communityService.getUserLikedPosts(userId, page, size));
    }
}
