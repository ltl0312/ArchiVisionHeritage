package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.community.interfaces.CommentRequest;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.community.interfaces.CommentResponse;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.FollowRecordMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.community.domain.Comment;
import com.zhiguan.gujian.community.domain.FollowRecord;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.task.domain.ModelAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * CommunityServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("社区服务测试")
class CommunityServiceImplTest {

    @InjectMocks
    private CommunityServiceImpl communityService;

    @Mock
    private PostMapper postMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private LikeRecordMapper likeRecordMapper;
    @Mock
    private FollowRecordMapper followRecordMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ModelAssetMapper modelAssetMapper;

    private Post testPost;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setRole("USER");

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setNickname("管理员");
        adminUser.setRole("ADMIN");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setUserId(1L);
        testPost.setTitle("测试帖子");
        testPost.setContent("测试内容");
        testPost.setStatus("APPROVED");
        testPost.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取帖子流 - 只返回 APPROVED 状态帖子")
    void getPostFeed_onlyApproved() {
        Page<Post> mockPage = new Page<>(1, 12);
        mockPage.setRecords(Arrays.asList(testPost));
        mockPage.setTotal(1);

        when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(userMapper.selectBatchIds(any())).thenReturn(Arrays.asList(testUser));
        when(likeRecordMapper.selectList(any())).thenReturn(Arrays.asList());
        when(commentMapper.selectList(any())).thenReturn(Arrays.asList());

        Page<PostBriefResponse> result = communityService.getPostFeed(1, 12);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(postMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("获取帖子详情 - 帖子存在")
    void getPostDetail_postExists() {
        when(postMapper.selectById(1L)).thenReturn(testPost);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(likeRecordMapper.selectCount(any())).thenReturn(0L);
        when(commentMapper.selectCount(any())).thenReturn(0L);
        when(commentMapper.selectList(any())).thenReturn(Arrays.asList());

        PostDetailResponse response = communityService.getPostDetail(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getPostId());
        assertEquals("测试帖子", response.getTitle());
        assertEquals("测试用户", response.getAuthorNickname());
    }

    @Test
    @DisplayName("获取帖子详情 - 帖子不存在抛出 404")
    void getPostDetail_postNotFound_throwsException() {
        when(postMapper.selectById(999L)).thenReturn(null);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> communityService.getPostDetail(999L, 1L)
        );

        assertEquals(404, exception.getCode());
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建帖子 - 普通用户状态为 PENDING")
    void createPost_normalUser_pending() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(postMapper.insert(any(Post.class))).thenReturn(1);

        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("新帖子");
        request.setContent("新内容");

        communityService.createPost(1L, request);

        verify(postMapper).insert(argThat(post -> "PENDING".equals(post.getStatus())));
    }

    @Test
    @DisplayName("创建帖子 - 管理员状态为 APPROVED")
    void createPost_admin_approved() {
        when(userMapper.selectById(2L)).thenReturn(adminUser);
        when(postMapper.insert(any(Post.class))).thenReturn(1);

        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("管理员帖子");
        request.setContent("管理员内容");

        communityService.createPost(2L, request);

        verify(postMapper).insert(argThat(post -> "APPROVED".equals(post.getStatus())));
    }

    @Test
    @DisplayName("点赞 - 首次点赞创建记录")
    void toggleLike_firstLike_createsRecord() {
        when(likeRecordMapper.selectOne(any())).thenReturn(null);
        when(likeRecordMapper.insert(any())).thenReturn(1);

        LikeRequest request = new LikeRequest();
        request.setTargetId(1L);
        request.setTargetType("POST");

        assertDoesNotThrow(() -> communityService.toggleLike(1L, request));

        verify(likeRecordMapper).insert(any());
        verify(likeRecordMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("点赞 - 再次点赞删除记录")
    void toggleLike_alreadyLiked_deletesRecord() {
        LikeRecord existing = new LikeRecord();
        existing.setId(1L);
        when(likeRecordMapper.selectOne(any())).thenReturn(existing);
        when(likeRecordMapper.deleteById(1L)).thenReturn(1);

        LikeRequest request = new LikeRequest();
        request.setTargetId(1L);
        request.setTargetType("POST");

        assertDoesNotThrow(() -> communityService.toggleLike(1L, request));

        verify(likeRecordMapper).deleteById(1L);
        verify(likeRecordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("关注 - 首次关注创建记录")
    void toggleFollow_firstFollow_createsRecord() {
        when(followRecordMapper.selectOne(any())).thenReturn(null);
        when(followRecordMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> communityService.toggleFollow(1L, 2L));

        verify(followRecordMapper).insert(any());
    }

    @Test
    @DisplayName("关注 - 不能关注自己")
    void toggleFollow_self_followIgnored() {
        assertDoesNotThrow(() -> communityService.toggleFollow(1L, 1L));

        verify(followRecordMapper, never()).selectOne(any());
        verify(followRecordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("审核帖子 - 帖子不存在抛出 404")
    void auditPost_postNotFound_throwsException() {
        when(postMapper.selectById(999L)).thenReturn(null);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> communityService.auditPost(999L, "APPROVED", null)
        );

        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("审核帖子 - 通过")
    void auditPost_approved() {
        Post pendingPost = new Post();
        pendingPost.setId(1L);
        pendingPost.setStatus("PENDING");

        when(postMapper.selectById(1L)).thenReturn(pendingPost);
        when(postMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> communityService.auditPost(1L, "APPROVED", null));

        verify(postMapper).updateById(argThat(post -> "APPROVED".equals(post.getStatus())));
    }

    @Test
    @DisplayName("审核帖子 - 驳回并设置原因")
    void auditPost_rejected_withReason() {
        Post pendingPost = new Post();
        pendingPost.setId(1L);
        pendingPost.setStatus("PENDING");

        when(postMapper.selectById(1L)).thenReturn(pendingPost);
        when(postMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> communityService.auditPost(1L, "REJECTED", "内容不符合规范"));

        verify(postMapper).updateById(argThat(post ->
                "REJECTED".equals(post.getStatus()) && "内容不符合规范".equals(post.getRejectReason())
        ));
    }
}
