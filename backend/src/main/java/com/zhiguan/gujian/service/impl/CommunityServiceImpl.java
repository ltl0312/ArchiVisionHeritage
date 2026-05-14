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

    /**
     * 获取帖子流 — 仅展示审核通过的帖子 (status = APPROVED)
     * 首页瀑布流按创建时间倒序，实现内容分发展示
     */
    @Override
    public Page<PostBriefResponse> getPostFeed(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "APPROVED")  // 仅展示已审核通过的帖子
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);

        return buildPostBriefPage(result, page, size);
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

    /**
     * 用户发帖 — 状态默认为 PENDING（待管理员审核）
     * 审核通过后才能在社区信息流中展示
     */
    @Override
    @Transactional
    public void createPost(Long userId, CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setModelAssetId(request.getModelAssetId());
        post.setStatus("PENDING");  // 默认待审核，需管理员审核后方可展示
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

    /**
     * 点赞/取消点赞 — 防抖逻辑：
     * 同一用户对同一目标的重复请求自动切换（有则删除/无则新增），
     * 利用数据库 UNIQUE KEY (user_id, target_id, target_type) 防止重复写入。
     */
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

    // ======================== 管理员审核 ========================

    /**
     * 管理员审核帖子 — 将帖子状态更新为 APPROVED 或 REJECTED
     * 仅 REJECTED 时记录驳回原因，APPROVED 后帖子出现在社区信息流
     */
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

    /**
     * 管理员获取待审核帖子列表 (status = PENDING)
     */
    @Override
    public Page<PostBriefResponse> getPendingPosts(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "PENDING")
                .orderByAsc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);

        return buildPostBriefPage(result, page, size);
    }

    /**
     * 获取用户发布的所有帖子（含各审核状态）
     */
    @Override
    public Page<PostBriefResponse> getUserPosts(Long userId, int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);

        return buildPostBriefPage(result, page, size);
    }

    /**
     * 获取用户点赞过的帖子 (通过 like_record 表反向查询)
     */
    @Override
    public Page<PostBriefResponse> getUserLikedPosts(Long userId, int page, int size) {
        // 先查用户点赞的所有 POST 类型记录
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

        // 分页截取
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, likedPostIds.size());
        if (fromIndex >= likedPostIds.size()) return emptyPage;

        List<Long> pageIds = likedPostIds.subList(fromIndex, toIndex);
        List<Post> posts = postMapper.selectBatchIds(pageIds);

        List<PostBriefResponse> records = posts.stream().map(this::toBriefResponse).collect(Collectors.toList());
        emptyPage.setRecords(records);
        return emptyPage;
    }

    // ======================== 内部工具方法 ========================

    private Page<PostBriefResponse> buildPostBriefPage(Page<Post> postPage, int page, int size) {
        Page<PostBriefResponse> responsePage = new Page<>(page, size, postPage.getTotal());
        responsePage.setRecords(postPage.getRecords().stream()
                .map(this::toBriefResponse)
                .collect(Collectors.toList()));
        return responsePage;
    }

    private PostBriefResponse toBriefResponse(Post post) {
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
    }
}
