package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;

public interface PostService {

    /** 获取帖子流 — 仅展示 APPROVED 状态的帖子 */
    Page<PostBriefResponse> getPostFeed(int page, int size);

    PostDetailResponse getPostDetail(Long postId, Long currentUserId);

    void createPost(Long userId, CreatePostRequest request);

    /** 管理员审核帖子：status = APPROVED 或 REJECTED */
    void auditPost(Long postId, String status, String rejectReason);

    /** 管理员获取待审核帖子列表 (status = PENDING) */
    Page<PostBriefResponse> getPendingPosts(int page, int size);

    /** 获取用户发布的所有帖子 */
    Page<PostBriefResponse> getUserPosts(Long userId, int page, int size);
}
