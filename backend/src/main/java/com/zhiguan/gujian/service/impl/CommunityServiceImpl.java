package com.zhiguan.gujian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.dto.request.CommentRequest;
import com.zhiguan.gujian.dto.request.CreatePostRequest;
import com.zhiguan.gujian.dto.request.LikeRequest;
import com.zhiguan.gujian.dto.response.CommentResponse;
import com.zhiguan.gujian.dto.response.PostBriefResponse;
import com.zhiguan.gujian.dto.response.PostDetailResponse;
import com.zhiguan.gujian.exception.CulturalApiException;
import com.zhiguan.gujian.mapper.*;
import com.zhiguan.gujian.model.*;
import com.zhiguan.gujian.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FollowRecordMapper followRecordMapper;
    private final UserMapper userMapper;
    private final ModelAssetMapper modelAssetMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public Page<PostBriefResponse> getPostFeed(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);

        Page<PostBriefResponse> responsePage = new Page<>(page, size, result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(post -> {
            User author = userMapper.selectById(post.getUserId());
            String preview2dPath = null;
            if (post.getModelAssetId() != null) {
                ModelAsset asset = modelAssetMapper.selectById(post.getModelAssetId());
                if (asset != null) preview2dPath = asset.getPreview2dPath();
            }

            int likeCount = likeRecordMapper.selectCount(
                    new LambdaQueryWrapper<LikeRecord>()
                            .eq(LikeRecord::getTargetId, post.getId())
                            .eq(LikeRecord::getTargetType, "POST")).intValue();

            int commentCount = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getPostId, post.getId())).intValue();

            return PostBriefResponse.builder()
                    .postId(post.getId())
                    .title(post.getTitle())
                    .preview2dPath(preview2dPath)
                    .authorNickname(author != null ? author.getNickname() : "未知")
                    .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "")
                    .build();
        }).collect(Collectors.toList()));

        return responsePage;
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new CulturalApiException(404, "帖子不存在");

        User author = userMapper.selectById(post.getUserId());
        String preview2dPath = null, glb3dPath = null;
        if (post.getModelAssetId() != null) {
            ModelAsset asset = modelAssetMapper.selectById(post.getModelAssetId());
            if (asset != null) {
                preview2dPath = asset.getPreview2dPath();
                glb3dPath = asset.getGlb3dPath();
            }
        }

        int likeCount = likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getTargetId, post.getId())
                        .eq(LikeRecord::getTargetType, "POST")).intValue();

        boolean likedByMe = currentUserId != null && likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, currentUserId)
                        .eq(LikeRecord::getTargetId, post.getId())
                        .eq(LikeRecord::getTargetType, "POST")) > 0;

        List<CommentResponse> comments = getComments(postId);

        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .preview2dPath(preview2dPath)
                .glb3dPath(glb3dPath)
                .authorNickname(author != null ? author.getNickname() : "未知")
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .comments(comments)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "")
                .build();
    }

    @Override
    @Transactional
    public void createPost(Long userId, CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setModelAssetId(request.getModelAssetId());
        postMapper.insert(post);
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long userId, Long postId, CommentRequest request) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(userId)
                .nickname(user != null ? user.getNickname() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .content(comment.getContent())
                .likeCount(0)
                .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().format(FMT) : "")
                .build();
    }

    @Override
    public List<CommentResponse> getComments(Long postId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .orderByAsc(Comment::getCreatedAt));

        return comments.stream().map(c -> {
            User user = userMapper.selectById(c.getUserId());
            int likes = likeRecordMapper.selectCount(
                    new LambdaQueryWrapper<LikeRecord>()
                            .eq(LikeRecord::getTargetId, c.getId())
                            .eq(LikeRecord::getTargetType, "COMMENT")).intValue();
            return CommentResponse.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .nickname(user != null ? user.getNickname() : "")
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .content(c.getContent())
                    .likeCount(likes)
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "")
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleLike(Long userId, LikeRequest request) {
        LikeRecord existing = likeRecordMapper.selectOne(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId)
                        .eq(LikeRecord::getTargetId, request.getTargetId())
                        .eq(LikeRecord::getTargetType, request.getTargetType()));

        if (existing != null) {
            likeRecordMapper.deleteById(existing.getId());
        } else {
            LikeRecord like = new LikeRecord();
            like.setUserId(userId);
            like.setTargetId(request.getTargetId());
            like.setTargetType(request.getTargetType());
            likeRecordMapper.insert(like);
        }
    }

    @Override
    @Transactional
    public void toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) return;

        FollowRecord existing = followRecordMapper.selectOne(
                new LambdaQueryWrapper<FollowRecord>()
                        .eq(FollowRecord::getFollowerId, followerId)
                        .eq(FollowRecord::getFollowingId, followingId));

        if (existing != null) {
            followRecordMapper.deleteById(existing.getId());
        } else {
            FollowRecord follow = new FollowRecord();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            followRecordMapper.insert(follow);
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) return false;
        return followRecordMapper.selectCount(
                new LambdaQueryWrapper<FollowRecord>()
                        .eq(FollowRecord::getFollowerId, followerId)
                        .eq(FollowRecord::getFollowingId, followingId)) > 0;
    }

    @Override
    public int getFollowerCount(Long userId) {
        return followRecordMapper.selectCount(
                new LambdaQueryWrapper<FollowRecord>()
                        .eq(FollowRecord::getFollowingId, userId)).intValue();
    }

    @Override
    public int getFollowingCount(Long userId) {
        return followRecordMapper.selectCount(
                new LambdaQueryWrapper<FollowRecord>()
                        .eq(FollowRecord::getFollowerId, userId)).intValue();
    }
}
