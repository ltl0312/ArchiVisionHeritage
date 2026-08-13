package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.Comment;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.interfaces.CommentRequest;
import com.zhiguan.gujian.community.interfaces.CommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
}
