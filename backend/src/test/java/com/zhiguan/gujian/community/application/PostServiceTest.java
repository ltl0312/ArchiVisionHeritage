package com.zhiguan.gujian.community.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.Post;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.FollowRecordMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.CreatePostRequest;
import com.zhiguan.gujian.community.interfaces.PostBriefResponse;
import com.zhiguan.gujian.community.interfaces.PostDetailResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PostServiceImpl 单元测试 — 原 CommunityServiceImplTest 的 feed/detail/create/audit 部分
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("帖子服务测试")
class PostServiceTest {

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
    @Mock
    private CommentService commentService;

    private PostBriefAssembler assembler;
    private PostServiceImpl postService;

    private Post testPost;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // 真实组装器（复用批量加载逻辑），依赖的 mapper 用上面的 Mock；
        // 不用 @InjectMocks：assembler 为 4 参构造，Mockito 无法自动注入
        assembler = new PostBriefAssembler(userMapper, modelAssetMapper, likeRecordMapper, commentMapper);
        postService = new PostServiceImpl(postMapper, userMapper, modelAssetMapper, likeRecordMapper,
                followRecordMapper, commentMapper, commentService, assembler);

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

        Page<PostBriefResponse> result = postService.getPostFeed(1, 12);

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
        when(commentService.getComments(anyLong())).thenReturn(Arrays.asList());

        PostDetailResponse response = postService.getPostDetail(1L, 1L);

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
                () -> postService.getPostDetail(999L, 1L)
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

        postService.createPost(1L, request);

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

        postService.createPost(2L, request);

        verify(postMapper).insert(argThat(post -> "APPROVED".equals(post.getStatus())));
    }

    @Test
    @DisplayName("审核帖子 - 帖子不存在抛出 404")
    void auditPost_postNotFound_throwsException() {
        when(postMapper.selectById(999L)).thenReturn(null);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> postService.auditPost(999L, "APPROVED", null)
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

        assertDoesNotThrow(() -> postService.auditPost(1L, "APPROVED", null));

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

        assertDoesNotThrow(() -> postService.auditPost(1L, "REJECTED", "内容不符合规范"));

        verify(postMapper).updateById(argThat(post ->
                "REJECTED".equals(post.getStatus()) && "内容不符合规范".equals(post.getRejectReason())
        ));
    }
}
