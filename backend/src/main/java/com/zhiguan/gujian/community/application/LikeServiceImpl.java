package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.task.domain.ModelAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRecordMapper likeRecordMapper;
    private final PostMapper postMapper;
    private final PostBriefAssembler postBriefAssembler;

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
        Map<Long, User> userMap = postBriefAssembler.batchLoadUsers(posts);
        Map<Long, ModelAsset> assetMap = postBriefAssembler.batchLoadModelAssets(posts);
        Map<Long, Integer> likeCountMap = postBriefAssembler.batchCountLikes(posts);
        Map<Long, Integer> commentCountMap = postBriefAssembler.batchCountComments(posts);

        List<PostBriefResponse> records = posts.stream()
                .map(p -> postBriefAssembler.toBriefResponse(p, userMap, assetMap, likeCountMap, commentCountMap))
                .collect(Collectors.toList());
        emptyPage.setRecords(records);
        return emptyPage;
    }
}
