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

    @Override
    public Page<PostBriefResponse> getPostFeed(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "APPROVED")
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        return postBriefAssembler.buildPostBriefPage(result);
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new CulturalApiException(404, "帖子不存在");

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
            post.setStatus("APPROVED");
        } else {
            post.setStatus("PENDING");
        }
        postMapper.insert(post);
    }

    @Override
    @Transactional
    public void auditPost(Long postId, String status, String rejectReason) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new CulturalApiException(404, "帖子不存在");

        post.setStatus(status);
        if ("REJECTED".equals(status)) {
            post.setRejectReason(rejectReason != null ? rejectReason : "内容不符合社区规范");
        } else {
            post.setRejectReason(null);
        }
        postMapper.updateById(post);
    }

    @Override
    public Page<PostBriefResponse> getPendingPosts(int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "PENDING")
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
