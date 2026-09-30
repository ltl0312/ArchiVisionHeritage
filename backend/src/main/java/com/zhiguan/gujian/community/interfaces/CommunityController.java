package com.zhiguan.gujian.community.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.shared.security.CallerIdentity;
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

/**
 * 社区控制器。
 *
 * 身份读取统一走 {@link CallerIdentity}（原先是散落的 {@code (Long) auth.getPrincipal()}）：
 * 既消除 ClassCastException 隐患，也让「需要登录」的端点有一致的 401 语义。
 */
@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final PostService postService;
    private final CommentService commentService;
    private final LikeService likeService;
    private final FollowService followService;

    // ======================== 社区帖子流 ========================

    /**
     * 获取帖子流（仅展示 APPROVED 帖子）。
     * 匿名也可访问；已登录时附带 likedByMe，供前端正确渲染点赞态。
     */
    @GetMapping("/api/v1/posts")
    public Result<Page<PostBriefResponse>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        return Result.ok(postService.getPostFeed(page, size, CallerIdentity.userId(auth)));
    }

    /**
     * 获取帖子详情。
     *
     * 该端点是 permitAll，因此必须自带可见性判断：只有 APPROVED 对所有人开放，
     * PENDING / REJECTED 仅作者本人与管理员可见（否则任何人都能枚举 id 读到未审核内容）。
     */
    @GetMapping("/api/v1/posts/{id}")
    public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, Authentication auth) {
        return Result.ok(postService.getPostDetail(id, CallerIdentity.userId(auth), CallerIdentity.isAdmin(auth)));
    }

    /** 发表帖子 — 默认状态 PENDING，待管理员审核 */
    @PostMapping("/api/v1/posts")
    public Result<Void> createPost(@Valid @RequestBody CreatePostRequest request, Authentication auth) {
        postService.createPost(CallerIdentity.requireUserId(auth), request);
        return Result.ok();
    }

    /** 发表评论 */
    @PostMapping("/api/v1/posts/{id}/comments")
    public Result<CommentResponse> addComment(@PathVariable Long id,
                                               @Valid @RequestBody CommentRequest request,
                                               Authentication auth) {
        return Result.ok(commentService.addComment(CallerIdentity.requireUserId(auth), id, request));
    }

    /** 点赞/取消点赞 (防抖) */
    @PostMapping("/api/v1/interactions/like")
    public Result<Void> toggleLike(@Valid @RequestBody LikeRequest request, Authentication auth) {
        likeService.toggleLike(CallerIdentity.requireUserId(auth), request);
        return Result.ok();
    }

    /** 关注/取消关注 */
    @PostMapping("/api/v1/users/{id}/follow")
    public Result<Void> toggleFollow(@PathVariable Long id, Authentication auth) {
        followService.toggleFollow(CallerIdentity.requireUserId(auth), id);
        return Result.ok();
    }

    // ======================== 个人中心帖子查询 ========================
    // 注：个人资料/密码接口已迁至 auth BC 的 ProfileController（URL 契约不变）

    /** 获取当前用户发布的帖子（含待审/已驳回，供「我的档案」展示审核状态与驳回原因） */
    @GetMapping("/api/v1/users/me/posts")
    public Result<Page<PostBriefResponse>> getMyPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        Long userId = CallerIdentity.requireUserId(auth);
        return Result.ok(postService.getUserPosts(userId, page, size, userId));
    }

    /** 获取当前用户点赞过的帖子 */
    @GetMapping("/api/v1/users/me/likes")
    public Result<Page<PostBriefResponse>> getMyLikedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        return Result.ok(likeService.getUserLikedPosts(CallerIdentity.requireUserId(auth), page, size));
    }
}
