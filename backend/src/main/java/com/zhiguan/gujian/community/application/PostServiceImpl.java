package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.Comment;
import com.zhiguan.gujian.community.domain.FollowRecord;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.FollowRecordMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.CommentResponse;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.task.domain.ModelAsset;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final ModelAssetMapper modelAssetMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FollowRecordMapper followRecordMapper;
    private final CommentMapper commentMapper;
    private final CommentService commentService;
    private final PostBriefAssembler postBriefAssembler;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 审核状态机常量 —— 原先散落为字面量，易拼错且无法统一校验 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final Set<String> AUDIT_STATUSES = Set.of(STATUS_APPROVED, STATUS_REJECTED);

    /**
     * 详情可见性：APPROVED 对所有人公开；PENDING / REJECTED 仅作者本人与管理员可见。
     * 这正是原先的漏洞 —— 详情接口不校验状态，而 GET /api/v1/posts/** 是 permitAll，
     * 于是任何人枚举 id 就能读到尚未通过审核、甚至已被驳回的内容。
     */
    private boolean isVisibleTo(Post post, Long currentUserId, boolean isAdmin) {
        if (STATUS_APPROVED.equals(post.getStatus())) {
            return true;
        }
        if (isAdmin) {
            return true;
        }
        return currentUserId != null && currentUserId.equals(post.getUserId());
    }

    @Override
    public Page<PostBriefResponse> getPostFeed(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, STATUS_APPROVED)
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return postBriefAssembler.buildPostBriefPage(result);
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId, Long currentUserId, boolean isAdmin) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new CulturalApiException(404, "帖子不存在");

        // 可见性规则：只有 APPROVED 对外公开；PENDING / REJECTED 仅作者本人与管理员可见。
        // 不公开的内容一律返回 404（而不是 403）—— 403 会暴露"这个 id 存在但未公开"，
        // 使任何人都能枚举出待审内容的存在性。
        if (!isVisibleTo(post, currentUserId, isAdmin)) {
            throw new CulturalApiException(404, "帖子不存在");
        }

        User author = userMapper.selectById(post.getUserId());
        String preview2dPath = null, glb3dPath = null;
        if (post.getModelAssetId() != null) {
            ModelAsset asset = modelAssetMapper.selectById(post.getModelAssetId());
            if (asset != null) {
                preview2dPath = asset.getPreview2dPath();
                glb3dPath = asset.getGlb3dPath();
            }
        }
        if (preview2dPath == null) preview2dPath = post.getCoverImageUrl();

        int likeCount = likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getTargetId, post.getId())
                        .eq(LikeRecord::getTargetType, "POST")).intValue();

        int commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, post.getId())).intValue();

        boolean likedByMe = currentUserId != null && likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, currentUserId)
                        .eq(LikeRecord::getTargetId, post.getId())
                        .eq(LikeRecord::getTargetType, "POST")) > 0;

        boolean followedByMe = currentUserId != null && followRecordMapper.selectCount(
                new LambdaQueryWrapper<FollowRecord>()
                        .eq(FollowRecord::getFollowerId, currentUserId)
                        .eq(FollowRecord::getFollowingId, post.getUserId())) > 0;

        List<CommentResponse> comments = commentService.getComments(postId);

        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .preview2dPath(preview2dPath)
                .glb3dPath(glb3dPath)
                .authorId(post.getUserId())
                .authorNickname(author != null ? author.getNickname() : "未知")
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByMe(likedByMe)
                .followedByMe(followedByMe)
                .tags(post.getTags())
                .comments(comments)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "")
                .build();
    }

    @Override
    @Transactional
    public void createPost(Long userId, CreatePostRequest request) {
        User user = userMapper.selectById(userId);

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setModelAssetId(request.getModelAssetId());
        post.setCoverImageUrl(request.getPreview2dPath());
        post.setTags(request.getTags());

        // 管理员发帖无需审核，直接发布
        if (user != null && "ADMIN".equals(user.getRole())) {
            post.setStatus(STATUS_APPROVED);
        } else {
            post.setStatus(STATUS_PENDING);
        }
        postMapper.insert(post);
    }

    @Override
    @Transactional
    public void auditPost(Long postId, String status, String rejectReason) {
        // 目标状态白名单（Controller 已校验一次，这里再校验一次：service 也可能被内部调用）
        // 注意 status 必须先判 null —— Set.of(...).contains(null) 会抛 NPE，
        // 那会把「参数非法」变成 500 而不是 400。
        if (status == null || !AUDIT_STATUSES.contains(status)) {
            throw new CulturalApiException(400, "status 必须为 APPROVED 或 REJECTED");
        }

        Post post = postMapper.selectById(postId);
        if (post == null) throw new CulturalApiException(404, "帖子不存在");

        // 状态机约束：只有 PENDING 可以被审核。
        // 原先此处无条件覆盖 status，导致已发布的内容可被反复改判、甚至改回 PENDING。
        if (!STATUS_PENDING.equals(post.getStatus())) {
            throw new CulturalApiException(409,
                    "该档案当前状态为 " + post.getStatus() + "，仅待审核（PENDING）的档案可被审核");
        }

        boolean rejecting = STATUS_REJECTED.equals(status);
        // 驳回必须填写理由 —— 接口文档与前端 UI 都是这么约定的，后端此前并未强制
        if (rejecting && (rejectReason == null || rejectReason.isBlank())) {
            throw new CulturalApiException(400, "驳回必须填写理由，理由将同步至作者站内信");
        }

        post.setStatus(status);
        post.setRejectReason(rejecting ? rejectReason.trim() : null);
        postMapper.updateById(post);
    }

    @Override
    public Page<PostBriefResponse> getPendingPosts(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, STATUS_PENDING)
                .orderByAsc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return postBriefAssembler.buildPostBriefPage(result);
    }

    @Override
    public Page<PostBriefResponse> getUserPosts(Long userId, int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return postBriefAssembler.buildPostBriefPage(result);
    }
}
