package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.Comment;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.CommentRequest;
import com.zhiguan.gujian.community.interfaces.CommentResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String STATUS_APPROVED = "APPROVED";

    @Override
    @Transactional
    public CommentResponse addComment(Long userId, Long postId, CommentRequest request) {
        // 帖子必须存在且已发布 —— 原先不校验，可为不存在的 postId 创建评论
        // （实测会撞外键约束并兜底成 500），也可给待审内容评论。
        Post post = postMapper.selectById(postId);
        if (post == null || !STATUS_APPROVED.equals(post.getStatus())) {
            throw new CulturalApiException(404, "帖子不存在");
        }

        // 回复的目标评论必须存在，且必须属于同一个帖子（防止跨帖挂载）
        Long parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !postId.equals(parent.getPostId())) {
                throw new CulturalApiException(400, "要回复的评论不存在");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(parentId);
        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(userId)
                .nickname(user != null ? user.getNickname() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .content(comment.getContent())
                .likeCount(0)
                .parentId(parentId)
                .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().format(FMT) : "")
                .build();
    }

    /**
     * 获取帖子的评论 —— 组装为**两级**结构（顶级评论 + 其 replies）。
     *
     * 原先无论 parentId 是什么都返回平铺列表，导致"回复"在界面上看不出层级。
     * 这里只做两级：
     *   · parentId 为 null，或其父评论已不存在 → 作为顶级评论
     *   · 父评论本身是回复（"回复的回复"）→ 挂到该顶级评论下，避免无限嵌套
     */
    @Override
    public List<CommentResponse> getComments(Long postId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .orderByAsc(Comment::getCreatedAt));

        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

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

        // 先为每条评论建出响应对象（保留插入顺序）
        Map<Long, CommentResponse> byId = new LinkedHashMap<>();
        for (Comment c : comments) {
            User user = userMap.get(c.getUserId());
            byId.put(c.getId(), CommentResponse.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .nickname(user != null ? user.getNickname() : "")
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .content(c.getContent())
                    .likeCount(likeCountMap.getOrDefault(c.getId(), 0))
                    .parentId(c.getParentId())
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "")
                    .build());
        }

        // 组装两级结构
        Map<Long, Comment> entityById = comments.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        List<CommentResponse> roots = new ArrayList<>();
        for (Comment c : comments) {
            CommentResponse response = byId.get(c.getId());
            Long parentId = c.getParentId();

            if (parentId == null || !byId.containsKey(parentId)) {
                // 顶级评论；父评论已被删除的孤儿回复也提升为顶级，避免"丢失的评论"
                roots.add(response);
                continue;
            }

            // 只保留两级：若父评论本身是回复，则挂到它所属的顶级评论下
            Comment parentEntity = entityById.get(parentId);
            Long attachToId = (parentEntity != null && parentEntity.getParentId() != null
                    && byId.containsKey(parentEntity.getParentId()))
                    ? parentEntity.getParentId()
                    : parentId;

            byId.get(attachToId).getReplies().add(response);
        }

        return roots;
    }
}
