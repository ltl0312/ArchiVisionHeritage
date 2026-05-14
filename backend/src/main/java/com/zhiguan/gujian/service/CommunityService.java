package com.zhiguan.gujian.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.dto.request.CommentRequest;
import com.zhiguan.gujian.dto.request.CreatePostRequest;
import com.zhiguan.gujian.dto.request.LikeRequest;
import com.zhiguan.gujian.dto.response.CommentResponse;
import com.zhiguan.gujian.dto.response.PostBriefResponse;
import com.zhiguan.gujian.dto.response.PostDetailResponse;

import java.util.List;

public interface CommunityService {

    /** 获取帖子流 — 仅展示 APPROVED 状态的帖子 */
    Page<PostBriefResponse> getPostFeed(int page, int size);

    PostDetailResponse getPostDetail(Long postId, Long currentUserId);

    void createPost(Long userId, CreatePostRequest request);

    CommentResponse addComment(Long userId, Long postId, CommentRequest request);

    List<CommentResponse> getComments(Long postId);

    void toggleLike(Long userId, LikeRequest request);

    void toggleFollow(Long followerId, Long followingId);

    boolean isFollowing(Long followerId, Long followingId);

    int getFollowerCount(Long userId);

    int getFollowingCount(Long userId);

    /** 管理员审核帖子：status = APPROVED 或 REJECTED */
    void auditPost(Long postId, String status, String rejectReason);

    /** 管理员获取待审核帖子列表 (status = PENDING) */
    Page<PostBriefResponse> getPendingPosts(int page, int size);

    /** 获取用户发布的所有帖子 */
    Page<PostBriefResponse> getUserPosts(Long userId, int page, int size);

    /** 获取用户点赞过的帖子 */
    Page<PostBriefResponse> getUserLikedPosts(Long userId, int page, int size);
}
