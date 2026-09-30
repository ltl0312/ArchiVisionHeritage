package com.zhiguan.gujian.community.interfaces;

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
    private String status;
    private String tags;
    private String createdAt;

    /**
     * 关联的三维资产 ID。为 null 表示这是一条「实景解析」档案，非 null 表示「AI 幻筑」产物。
     * 前端据此区分「我的解析 / 我的幻筑」（原先该字段缺失，导致无法区分）。
     */
    private Long modelAssetId;

    /**
     * 驳回原因。仅对作者本人与管理员有意义 —— 调用方需保证不把它发给无关用户
     * （列表接口只返回 APPROVED 时该值恒为 null；我的档案/待审队列分别是作者与管理员视角）。
     */
    private String rejectReason;

    /**
     * 当前调用方是否已点赞。
     * ⚠️ 该字段原先缺失，而前端 `PostCard` 依赖它渲染点赞态 ——
     * 结果是信息流里点赞态恒为 false：已赞的帖子刷新后显示未赞，
     * 用户再点一次「赞赏」时乐观更新把 UI 置为已赞，而后端 toggle 实际执行的是**取消点赞**，
     * 造成点赞态反转。
     */
    private boolean likedByMe;
}
