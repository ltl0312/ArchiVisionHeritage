package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiguan.gujian.community.domain.FollowRecord;
import com.zhiguan.gujian.community.infrastructure.FollowRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRecordMapper followRecordMapper;

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
}
