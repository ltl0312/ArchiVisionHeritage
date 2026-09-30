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

    /**
     * 审核状态：PENDING / APPROVED / REJECTED。
     * 详情接口只对「已发布」或「作者本人/管理员」可见，因此暴露状态不会泄露未公开内容。
     */
    private String status;

    /**
     * 驳回原因。仅在 REJECTED 时有值，且只可能被作者本人或管理员读到
     * （可见性校验见 PostServiceImpl#getPostDetail）。原先该字段未暴露，
     * 作者被驳回后看不到原因。
     */
    private String rejectReason;
}
