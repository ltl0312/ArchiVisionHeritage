package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;

public interface LikeService {

    void toggleLike(Long userId, LikeRequest request);

    /** 获取用户点赞过的帖子 */
    Page<PostBriefResponse> getUserLikedPosts(Long userId, int page, int size);
}
