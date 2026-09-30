package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LikeServiceImpl 单元测试 — 原 CommunityServiceImplTest 的 toggleLike 部分
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("点赞服务测试")
class LikeServiceTest {

    @Mock
    private LikeRecordMapper likeRecordMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ModelAssetMapper modelAssetMapper;
    @Mock
    private CommentMapper commentMapper;

    private LikeServiceImpl likeService;

    @BeforeEach
    void setUp() {
        likeService = new LikeServiceImpl(likeRecordMapper, postMapper, commentMapper,
                new PostBriefAssembler(userMapper, modelAssetMapper, likeRecordMapper, commentMapper));
    }

    /** 点赞目标校验需要帖子存在且已发布 */
    private void stubApprovedPost(Long postId) {
        Post post = new Post();
        post.setId(postId);
        post.setStatus("APPROVED");
        when(postMapper.selectById(postId)).thenReturn(post);
    }

    @Test
    @DisplayName("点赞 - 首次点赞创建记录")
    void toggleLike_firstLike_createsRecord() {
        stubApprovedPost(1L);
        when(likeRecordMapper.selectOne(any())).thenReturn(null);
        when(likeRecordMapper.insert(any())).thenReturn(1);

        LikeRequest request = new LikeRequest();
        request.setTargetId(1L);
        request.setTargetType("POST");

        assertDoesNotThrow(() -> likeService.toggleLike(1L, request));

        verify(likeRecordMapper).insert(any());
        verify(likeRecordMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("点赞 - 再次点赞删除记录")
    void toggleLike_alreadyLiked_deletesRecord() {
        stubApprovedPost(1L);
        LikeRecord existing = new LikeRecord();
        existing.setId(1L);
        when(likeRecordMapper.selectOne(any())).thenReturn(existing);
        when(likeRecordMapper.deleteById(1L)).thenReturn(1);

        LikeRequest request = new LikeRequest();
        request.setTargetId(1L);
        request.setTargetType("POST");

        assertDoesNotThrow(() -> likeService.toggleLike(1L, request));

        verify(likeRecordMapper).deleteById(1L);
        verify(likeRecordMapper, never()).insert(any());
    }

    /* ═══════════════════════════════════════════════════════════════
       目标存在性校验（原先不校验 → 产生孤儿点赞；也能给未审核内容点赞，
       于是它出现在「我的收藏」里但详情是 404）
       ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("目标校验 - 给不存在的帖子点赞 → 404（原先 200，产生孤儿记录）")
    void toggleLike_nonexistentPost_throws404() {
        when(postMapper.selectById(999L)).thenReturn(null);

        LikeRequest request = new LikeRequest();
        request.setTargetId(999L);
        request.setTargetType("POST");

        CulturalApiException ex = assertThrows(CulturalApiException.class,
                () -> likeService.toggleLike(1L, request));
        assertEquals(404, ex.getCode());
        verify(likeRecordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("目标校验 - 给未审核（PENDING）的帖子点赞 → 404")
    void toggleLike_pendingPost_throws404() {
        Post pending = new Post();
        pending.setId(1L);
        pending.setStatus("PENDING");
        when(postMapper.selectById(1L)).thenReturn(pending);

        LikeRequest request = new LikeRequest();
        request.setTargetId(1L);
        request.setTargetType("POST");

        assertEquals(404, assertThrows(CulturalApiException.class,
                () -> likeService.toggleLike(1L, request)).getCode());
        verify(likeRecordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("目标校验 - 给不存在的评论点赞 → 404")
    void toggleLike_nonexistentComment_throws404() {
        when(commentMapper.selectById(999L)).thenReturn(null);

        LikeRequest request = new LikeRequest();
        request.setTargetId(999L);
        request.setTargetType("COMMENT");

        assertEquals(404, assertThrows(CulturalApiException.class,
                () -> likeService.toggleLike(1L, request)).getCode());
    }

    @Test
    @DisplayName("目标校验 - 非法 targetType → 400（service 层第二道防护）")
    void toggleLike_invalidTargetType_throws400() {
        LikeRequest request = new LikeRequest();
        request.setTargetId(1L);
        request.setTargetType("EVIL");

        assertEquals(400, assertThrows(CulturalApiException.class,
                () -> likeService.toggleLike(1L, request)).getCode());
        verify(likeRecordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("获取点赞帖子 - SQL 分页 + 批量装配")
    void getUserLikedPosts_usesSqlPagination() {
        LikeRecord like = new LikeRecord();
        like.setId(1L);
        like.setUserId(1L);
        like.setTargetId(10L);
        like.setTargetType("POST");

        Page<LikeRecord> likePage = new Page<>(1, 10, 1);
        likePage.setRecords(Collections.singletonList(like));
        when(likeRecordMapper.selectPageByUser(any(), eq(1L))).thenReturn(likePage);

        Post post = new Post();
        post.setId(10L);
        post.setUserId(2L);
        post.setTitle("应县木塔");
        post.setStatus("APPROVED");
        when(postMapper.selectBatchIds(Collections.singletonList(10L)))
                .thenReturn(Collections.singletonList(post));

        // 关联数据批量加载均为空（post 无 modelAssetId → batchLoadModelAssets 不触发，勿桩）
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(likeRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(commentMapper.selectList(any())).thenReturn(Collections.emptyList());

        Page<PostBriefResponse> result = likeService.getUserLikedPosts(1L, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(10L, result.getRecords().get(0).getPostId());
        assertEquals("应县木塔", result.getRecords().get(0).getTitle());
    }
}
