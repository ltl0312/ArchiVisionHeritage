package com.zhiguan.gujian.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 帖子详情 — 含3D GLB路径与评论列表
 */
@Data
@Builder
public class PostDetailResponse {
    private Long postId;
    private String title;
    private String content;
    private String preview2dPath;
    private String glb3dPath;
    private String authorNickname;
    private String authorAvatarUrl;
    private int likeCount;
    private boolean likedByMe;
    private List<CommentResponse> comments;
    private String createdAt;
}
