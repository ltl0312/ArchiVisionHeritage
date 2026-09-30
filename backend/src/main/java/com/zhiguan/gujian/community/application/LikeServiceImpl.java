package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.community.domain.Comment;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.task.domain.ModelAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRecordMapper likeRecordMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostBriefAssembler postBriefAssembler;

    @Override
    @Transactional
    public void toggleLike(Long userId, LikeRequest request) {
        // 目标必须存在且可见 —— 原先不校验，可给不存在的 id 点赞（产生孤儿记录），
        // 也可以给尚未通过审核的帖子点赞（于是它出现在「我的收藏」里但详情是 404）。
        assertTargetLikeable(request.getTargetType(), request.getTargetId());

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

    /** 点赞对象校验：POST 必须存在且已发布；COMMENT 必须存在 */
    private void assertTargetLikeable(String targetType, Long targetId) {
        if ("POST".equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            if (post == null || !"APPROVED".equals(post.getStatus())) {
                // 未公开的帖子按"不存在"处理，与详情接口的 404 语义保持一致
                throw new CulturalApiException(404, "帖子不存在");
            }
        } else if ("COMMENT".equals(targetType)) {
            if (commentMapper.selectById(targetId) == null) {
                throw new CulturalApiException(404, "评论不存在");
            }
        } else {
            // LikeRequest 上已有 @Pattern 白名单，这里是 service 层的第二道防护
            throw new CulturalApiException(400, "targetType 必须为 POST 或 COMMENT");
        }
    }

    @Override
    public Page<PostBriefResponse> getUserLikedPosts(Long userId, int page, int size) {
        // SQL 分页：ORDER BY created_at DESC + LIMIT 由分页插件追加（原内存分页改为 DB 分页）
        IPage<LikeRecord> likePage = likeRecordMapper.selectPageByUser(new Page<>(page, size), userId);

        Page<PostBriefResponse> responsePage = new Page<>(page, size, likePage.getTotal());
        List<Long> pageIds = likePage.getRecords().stream()
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());
        if (pageIds.isEmpty()) return responsePage;

        List<Post> posts = postMapper.selectBatchIds(pageIds);

        // 批量加载关联数据
        Map<Long, User> userMap = postBriefAssembler.batchLoadUsers(posts);
        Map<Long, ModelAsset> assetMap = postBriefAssembler.batchLoadModelAssets(posts);
        Map<Long, Integer> likeCountMap = postBriefAssembler.batchCountLikes(posts);
        Map<Long, Integer> commentCountMap = postBriefAssembler.batchCountComments(posts);

        // 「我的收藏」里的帖子按定义都是当前用户点过赞的
        Set<Long> likedIds = new HashSet<>(pageIds);

        List<PostBriefResponse> records = posts.stream()
                .map(p -> postBriefAssembler.toBriefResponse(
                        p, userMap, assetMap, likeCountMap, commentCountMap, likedIds))
                .collect(Collectors.toList());
        responsePage.setRecords(records);
        return responsePage;
    }
}
