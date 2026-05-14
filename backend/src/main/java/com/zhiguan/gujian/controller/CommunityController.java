package com.zhiguan.gujian.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.config.Result;
import com.zhiguan.gujian.dto.request.CommentRequest;
import com.zhiguan.gujian.dto.request.CreatePostRequest;
import com.zhiguan.gujian.dto.request.LikeRequest;
import com.zhiguan.gujian.dto.response.CommentResponse;
import com.zhiguan.gujian.dto.response.PostBriefResponse;
import com.zhiguan.gujian.dto.response.PostDetailResponse;
import com.zhiguan.gujian.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /** 获取帖子流（2D封面降维展示，分页） */
    @GetMapping("/posts")
    public Result<Page<PostBriefResponse>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(communityService.getPostFeed(page, size));
    }

    /** 获取帖子详情（含3D GLB路径与评论） */
    @GetMapping("/posts/{id}")
    public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, Authentication auth) {
        Long currentUserId = auth != null ? (Long) auth.getPrincipal() : null;
        return Result.ok(communityService.getPostDetail(id, currentUserId));
    }

    /** 发表帖子 */
    @PostMapping("/posts")
    public Result<Void> createPost(@Valid @RequestBody CreatePostRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        communityService.createPost(userId, request);
        return Result.ok();
    }

    /** 发表评论 */
    @PostMapping("/posts/{id}/comments")
    public Result<CommentResponse> addComment(@PathVariable Long id,
                                               @Valid @RequestBody CommentRequest request,
                                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(communityService.addComment(userId, id, request));
    }

    /** 点赞/取消点赞 */
    @PostMapping("/interactions/like")
    public Result<Void> toggleLike(@Valid @RequestBody LikeRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        communityService.toggleLike(userId, request);
        return Result.ok();
    }

    /** 关注/取消关注 */
    @PostMapping("/users/{id}/follow")
    public Result<Void> toggleFollow(@PathVariable Long id, Authentication auth) {
        Long followerId = (Long) auth.getPrincipal();
        communityService.toggleFollow(followerId, id);
        return Result.ok();
    }
}
