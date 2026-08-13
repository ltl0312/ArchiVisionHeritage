package com.zhiguan.gujian.community.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.community.interfaces.CommentRequest;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.CommentResponse;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;
import com.zhiguan.gujian.community.application.PostService;
import com.zhiguan.gujian.community.application.CommentService;
import com.zhiguan.gujian.community.application.LikeService;
import com.zhiguan.gujian.community.application.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final PostService postService;
    private final CommentService commentService;
    private final LikeService likeService;
    private final FollowService followService;

    // ======================== 社区帖子流 ========================

    /** 获取帖子流（仅展示 APPROVED 帖子） */
    @GetMapping("/api/v1/posts")
    public Result<Page<PostBriefResponse>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(postService.getPostFeed(page, size));
    }

    /** 获取帖子详情 */
    @GetMapping("/api/v1/posts/{id}")
    public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, Authentication auth) {
        Long currentUserId = null;
        if (auth != null && auth.getPrincipal() instanceof Long) {
            currentUserId = (Long) auth.getPrincipal();
        }
        return Result.ok(postService.getPostDetail(id, currentUserId));
    }

    /** 发表帖子 — 默认状态 PENDING，待管理员审核 */
    @PostMapping("/api/v1/posts")
    public Result<Void> createPost(@Valid @RequestBody CreatePostRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        postService.createPost(userId, request);
        return Result.ok();
    }

    /** 发表评论 */
    @PostMapping("/api/v1/posts/{id}/comments")
    public Result<CommentResponse> addComment(@PathVariable Long id,
                                               @Valid @RequestBody CommentRequest request,
                                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(commentService.addComment(userId, id, request));
    }

    /** 点赞/取消点赞 (防抖) */
    @PostMapping("/api/v1/interactions/like")
    public Result<Void> toggleLike(@Valid @RequestBody LikeRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        likeService.toggleLike(userId, request);
        return Result.ok();
    }

    /** 关注/取消关注 */
    @PostMapping("/api/v1/users/{id}/follow")
    public Result<Void> toggleFollow(@PathVariable Long id, Authentication auth) {
        Long followerId = (Long) auth.getPrincipal();
        followService.toggleFollow(followerId, id);
        return Result.ok();
    }

    // ======================== 个人中心帖子查询 ========================
    // 注：个人资料/密码接口已迁至 auth BC 的 ProfileController（URL 契约不变）

    /** 获取当前用户发布的帖子 */
    @GetMapping("/api/v1/users/me/posts")
    public Result<Page<PostBriefResponse>> getMyPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(postService.getUserPosts(userId, page, size));
    }

    /** 获取当前用户点赞过的帖子 */
    @GetMapping("/api/v1/users/me/likes")
    public Result<Page<PostBriefResponse>> getMyLikedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(likeService.getUserLikedPosts(userId, page, size));
    }
}
