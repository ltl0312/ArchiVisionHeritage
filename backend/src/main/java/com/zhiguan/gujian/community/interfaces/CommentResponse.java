package com.zhiguan.gujian.community.interfaces;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String content;
    private int likeCount;
    private String createdAt;
}
