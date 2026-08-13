package com.zhiguan.gujian.community.application;

public interface FollowService {

    void toggleFollow(Long followerId, Long followingId);
}
