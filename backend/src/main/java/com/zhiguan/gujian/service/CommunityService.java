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
}
