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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
                .eq(Post::getStatus, "APPROVED")
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return buildPostBriefPage(result);
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
        if (preview2dPath == null) preview2dPath = post.getCoverImageUrl();

        int likeCount = likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getTargetId, post.getId())
                        .eq(LikeRecord::getTargetType, "POST")).intValue();

        int commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, post.getId())).intValue();

        boolean likedByMe = currentUserId != null && likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, currentUserId)
                        .eq(LikeRecord::getTargetId, post.getId())
                        .eq(LikeRecord::getTargetType, "POST")) > 0;

        boolean followedByMe = currentUserId != null && followRecordMapper.selectCount(
                new LambdaQueryWrapper<FollowRecord>()
                        .eq(FollowRecord::getFollowerId, currentUserId)
                        .eq(FollowRecord::getFollowingId, post.getUserId())) > 0;

        List<CommentResponse> comments = getComments(postId);

        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .preview2dPath(preview2dPath)
                .glb3dPath(glb3dPath)
                .authorId(post.getUserId())
                .authorNickname(author != null ? author.getNickname() : "未知")
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByMe(likedByMe)
                .followedByMe(followedByMe)
                .tags(post.getTags())
                .comments(comments)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "")
                .build();
    }

    @Override
    @Transactional
    public void createPost(Long userId, CreatePostRequest request) {
        User user = userMapper.selectById(userId);

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setModelAssetId(request.getModelAssetId());
        post.setCoverImageUrl(request.getPreview2dPath());
        post.setTags(request.getTags());

        // 管理员发帖无需审核，直接发布
        if (user != null && "ADMIN".equals(user.getRole())) {
            post.setStatus("APPROVED");
        } else {
            post.setStatus("PENDING");
        }
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

        // 批量查询评论作者
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // 批量查询评论点赞数
        List<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Long, Integer> likeCountMap = new HashMap<>();
        if (!commentIds.isEmpty()) {
            List<LikeRecord> commentLikes = likeRecordMapper.selectList(
                    new LambdaQueryWrapper<LikeRecord>()
                            .in(LikeRecord::getTargetId, commentIds)
                            .eq(LikeRecord::getTargetType, "COMMENT"));
            for (LikeRecord lr : commentLikes) {
                likeCountMap.merge(lr.getTargetId(), 1, Integer::sum);
            }
        }

        return comments.stream().map(c -> {
            User user = userMap.get(c.getUserId());
            return CommentResponse.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .nickname(user != null ? user.getNickname() : "")
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .content(c.getContent())
                    .likeCount(likeCountMap.getOrDefault(c.getId(), 0))
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
            try {
                LikeRecord like = new LikeRecord();
                like.setUserId(userId);
                like.setTargetId(request.getTargetId());
                like.setTargetType(request.getTargetType());
                likeRecordMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发情况下可能重复插入，忽略即可
                log.debug("点赞记录已存在，忽略重复插入: userId={}, targetId={}", userId, request.getTargetId());
            }
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
            try {
                FollowRecord follow = new FollowRecord();
                follow.setFollowerId(followerId);
                follow.setFollowingId(followingId);
                followRecordMapper.insert(follow);
            } catch (DuplicateKeyException e) {
                // 并发情况下可能重复插入，忽略即可
                log.debug("关注记录已存在，忽略重复插入: followerId={}, followingId={}", followerId, followingId);
            }
        }
    }

    // ======================== 管理员审核 ========================

    @Override
    @Transactional
    public void auditPost(Long postId, String status, String rejectReason) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new CulturalApiException(404, "帖子不存在");

        post.setStatus(status);
        if ("REJECTED".equals(status)) {
            post.setRejectReason(rejectReason != null ? rejectReason : "内容不符合社区规范");
        } else {
            post.setRejectReason(null);
        }
        postMapper.updateById(post);
    }

    @Override
    public Page<PostBriefResponse> getPendingPosts(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "PENDING")
                .orderByAsc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return buildPostBriefPage(result);
    }

    @Override
    public Page<PostBriefResponse> getUserPosts(Long userId, int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return buildPostBriefPage(result);
    }

    @Override
    public Page<PostBriefResponse> getUserLikedPosts(Long userId, int page, int size) {
        List<LikeRecord> likes = likeRecordMapper.selectList(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId)
                        .eq(LikeRecord::getTargetType, "POST")
                        .orderByDesc(LikeRecord::getCreatedAt));

        List<Long> likedPostIds = likes.stream()
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());

        Page<PostBriefResponse> emptyPage = new Page<>(page, size, likedPostIds.size());
        if (likedPostIds.isEmpty()) return emptyPage;

        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, likedPostIds.size());
        if (fromIndex >= likedPostIds.size()) return emptyPage;

        List<Long> pageIds = likedPostIds.subList(fromIndex, toIndex);
        List<Post> posts = postMapper.selectBatchIds(pageIds);

        // 批量加载关联数据
        Map<Long, User> userMap = batchLoadUsers(posts);
        Map<Long, ModelAsset> assetMap = batchLoadModelAssets(posts);
        Map<Long, Integer> likeCountMap = batchCountLikes(posts);
        Map<Long, Integer> commentCountMap = batchCountComments(posts);

        List<PostBriefResponse> records = posts.stream()
                .map(p -> toBriefResponse(p, userMap, assetMap, likeCountMap, commentCountMap))
                .collect(Collectors.toList());
        emptyPage.setRecords(records);
        return emptyPage;
    }

    // ======================== 批量查询（消除 N+1） ========================

    private Page<PostBriefResponse> buildPostBriefPage(Page<Post> postPage) {
        List<Post> posts = postPage.getRecords();

        Map<Long, User> userMap = batchLoadUsers(posts);
        Map<Long, ModelAsset> assetMap = batchLoadModelAssets(posts);
        Map<Long, Integer> likeCountMap = batchCountLikes(posts);
        Map<Long, Integer> commentCountMap = batchCountComments(posts);

        List<PostBriefResponse> records = posts.stream()
                .map(p -> toBriefResponse(p, userMap, assetMap, likeCountMap, commentCountMap))
                .collect(Collectors.toList());

        Page<PostBriefResponse> responsePage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    private Map<Long, User> batchLoadUsers(List<Post> posts) {
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> map = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> map.put(u.getId(), u));
        }
        return map;
    }

    private Map<Long, ModelAsset> batchLoadModelAssets(List<Post> posts) {
        Set<Long> assetIds = posts.stream()
                .map(Post::getModelAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ModelAsset> map = new HashMap<>();
        if (!assetIds.isEmpty()) {
            modelAssetMapper.selectBatchIds(assetIds).forEach(a -> map.put(a.getId(), a));
        }
        return map;
    }

    private Map<Long, Integer> batchCountLikes(List<Post> posts) {
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Integer> map = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<LikeRecord> likes = likeRecordMapper.selectList(
                    new LambdaQueryWrapper<LikeRecord>()
                            .in(LikeRecord::getTargetId, postIds)
                            .eq(LikeRecord::getTargetType, "POST"));
            for (LikeRecord lr : likes) {
                map.merge(lr.getTargetId(), 1, Integer::sum);
            }
        }
        return map;
    }

    private Map<Long, Integer> batchCountComments(List<Post> posts) {
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Integer> map = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<Comment> comments = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>().in(Comment::getPostId, postIds));
            for (Comment c : comments) {
                map.merge(c.getPostId(), 1, Integer::sum);
            }
        }
        return map;
    }

    private PostBriefResponse toBriefResponse(Post post, Map<Long, User> userMap,
                                               Map<Long, ModelAsset> assetMap,
                                               Map<Long, Integer> likeCountMap,
                                               Map<Long, Integer> commentCountMap) {
        User author = userMap.get(post.getUserId());
        String preview2dPath = post.getCoverImageUrl();
        if (preview2dPath == null && post.getModelAssetId() != null) {
            ModelAsset asset = assetMap.get(post.getModelAssetId());
            if (asset != null) preview2dPath = asset.getPreview2dPath();
        }

        return PostBriefResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .preview2dPath(preview2dPath)
                .authorNickname(author != null ? author.getNickname() : "未知")
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .likeCount(likeCountMap.getOrDefault(post.getId(), 0))
                .commentCount(commentCountMap.getOrDefault(post.getId(), 0))
                .status(post.getStatus())
                .tags(post.getTags())
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "")
                .build();
    }
}
