package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;

public interface PostService {

    /** 获取帖子流 — 仅展示 APPROVED 状态的帖子 */
    Page<PostBriefResponse> getPostFeed(int page, int size);

    /**
     * 获取帖子详情。
     *
     * @param currentUserId 调用方 userId（匿名传 null）
     * @param isAdmin       调用方是否管理员
     * @return 详情；**非 APPROVED 的帖子仅作者本人与管理员可见**，其他人一律 404
     */
    PostDetailResponse getPostDetail(Long postId, Long currentUserId, boolean isAdmin);

    void createPost(Long userId, CreatePostRequest request);

    /**
     * 管理员审核帖子。
     *
     * 状态机约束（原先只在 javadoc 里声称、代码并未实现，已实测可把已发布内容任意改判）：
     * 仅 {@code PENDING} 可转移到 {@code APPROVED} / {@code REJECTED}；
     * 驳回必须填写理由（与接口文档一致）。
     */
    void auditPost(Long postId, String status, String rejectReason);

    /** 管理员获取待审核帖子列表 (status = PENDING) */
    Page<PostBriefResponse> getPendingPosts(int page, int size);

    /** 获取用户发布的所有帖子 */
    Page<PostBriefResponse> getUserPosts(Long userId, int page, int size);
}
