package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.Comment;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.task.domain.ModelAsset;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子摘要分页组装器 — 供 PostServiceImpl / LikeServiceImpl 复用，消除 N+1。
 * 包私有：仅 community.application 内部使用。
 */
@Component
final class PostBriefAssembler {

    private final UserMapper userMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final CommentMapper commentMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PostBriefAssembler(UserMapper userMapper, ModelAssetMapper modelAssetMapper,
                              LikeRecordMapper likeRecordMapper, CommentMapper commentMapper) {
        this.userMapper = userMapper;
        this.modelAssetMapper = modelAssetMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.commentMapper = commentMapper;
    }

    public Page<PostBriefResponse> buildPostBriefPage(Page<Post> postPage) {
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

    public Map<Long, User> batchLoadUsers(List<Post> posts) {
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> map = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> map.put(u.getId(), u));
        }
        return map;
    }

    public Map<Long, ModelAsset> batchLoadModelAssets(List<Post> posts) {
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

    public Map<Long, Integer> batchCountLikes(List<Post> posts) {
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

    public Map<Long, Integer> batchCountComments(List<Post> posts) {
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

    public PostBriefResponse toBriefResponse(Post post, Map<Long, User> userMap,
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
