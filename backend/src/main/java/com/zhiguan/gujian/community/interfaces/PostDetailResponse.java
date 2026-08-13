package com.zhiguan.gujian.community.interfaces;

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
    private Long authorId;
    private int likeCount;
    private int commentCount;
    private boolean likedByMe;
    private boolean followedByMe;
    private String tags;
    private List<CommentResponse> comments;
    private String createdAt;
}
