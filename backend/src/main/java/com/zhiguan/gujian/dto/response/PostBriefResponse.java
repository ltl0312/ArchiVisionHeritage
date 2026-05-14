package com.zhiguan.gujian.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 帖子流降维展示 — 仅含2D封面
 */
@Data
@Builder
public class PostBriefResponse {
    private Long postId;
    private String title;
    private String preview2dPath;
    private String authorNickname;
    private String authorAvatarUrl;
    private int likeCount;
    private int commentCount;
    private String createdAt;
}
