package com.zhiguan.gujian.community.interfaces;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    /** 父评论 id；为 null 表示顶级评论 */
    private Long parentId;

    /**
     * 子回复（**只做两级**）。
     *
     * 原先 `comment.parent_id` 被写进库却从未被读取，`getComments` 一律返回平铺列表 ——
     * 即"回复"功能是半成品：能提交回复，但界面上看不出回复关系。
     * 现在按 parentId 组装两级结构；对"回复的回复"统一挂到其所属顶级评论下（避免无限层级）。
     */
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}
