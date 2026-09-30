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

        PostDetailResponse response = postService.getPostDetail(1L, 1L, false);

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
                () -> postService.getPostDetail(999L, 1L, false)
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

    /* ═══════════════════════════════════════════════════════════════
       详情可见性 —— 修复「未审核/已驳回内容可被匿名读取」
       原实现 getPostDetail 只做 null 检查、不校验 status，而 GET /api/v1/posts/** 是
       permitAll，于是任何人枚举 id 就能读到尚未通过审核甚至已被驳回的内容（已实测）。
       ═══════════════════════════════════════════════════════════════ */

    private Post postWithStatus(String status, Long authorId) {
        Post p = new Post();
        p.setId(10L);
        p.setUserId(authorId);
        p.setTitle("待审草稿");
        p.setContent("内部草稿内容");
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private void stubDetailDependencies() {
        when(userMapper.selectById(anyLong())).thenReturn(testUser);
        when(likeRecordMapper.selectCount(any())).thenReturn(0L);
        when(commentMapper.selectCount(any())).thenReturn(0L);
        when(commentService.getComments(anyLong())).thenReturn(Arrays.asList());
    }

    @Test
    @DisplayName("详情可见性 - 待审帖：匿名访问 → 404（不泄露存在性）")
    void getPostDetail_pendingPost_anonymous_is404() {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus("PENDING", 1L));

        CulturalApiException ex = assertThrows(CulturalApiException.class,
                () -> postService.getPostDetail(10L, null, false));

        assertEquals(404, ex.getCode(), "应返回 404 而非 403，避免暴露「存在但未公开」");
    }

    @Test
    @DisplayName("详情可见性 - 待审帖：其他登录用户 → 404")
    void getPostDetail_pendingPost_otherUser_is404() {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus("PENDING", 1L));

        CulturalApiException ex = assertThrows(CulturalApiException.class,
                () -> postService.getPostDetail(10L, 99L, false));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("详情可见性 - 已驳回帖：匿名访问 → 404（审核不能被绕过）")
    void getPostDetail_rejectedPost_anonymous_is404() {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus("REJECTED", 1L));

        assertEquals(404, assertThrows(CulturalApiException.class,
                () -> postService.getPostDetail(10L, null, false)).getCode());
    }

    @Test
    @DisplayName("详情可见性 - 待审帖：作者本人可见")
    void getPostDetail_pendingPost_authorCanSee() {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus("PENDING", 1L));
        stubDetailDependencies();

        PostDetailResponse res = postService.getPostDetail(10L, 1L, false);

        assertEquals(10L, res.getPostId());
        assertEquals("待审草稿", res.getTitle());
    }

    @Test
    @DisplayName("详情可见性 - 待审帖：管理员可见（审核台需要）")
    void getPostDetail_pendingPost_adminCanSee() {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus("PENDING", 1L));
        stubDetailDependencies();

        assertNotNull(postService.getPostDetail(10L, 2L, true));
    }

    @Test
    @DisplayName("详情可见性 - 已发布帖：匿名可见")
    void getPostDetail_approvedPost_anonymousCanSee() {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus("APPROVED", 1L));
        stubDetailDependencies();

        assertNotNull(postService.getPostDetail(10L, null, false));
    }

    /* ═══════════════════════════════════════════════════════════════
       审核状态机 —— 修复「已发布/已驳回内容可被任意反复改判」
       AdminController 的 javadoc 声称 PENDING → APPROVED/REJECTED，
       但原实现只做 null 检查就无条件覆盖 status（已实测可把已发布内容改判、甚至改回 PENDING）。
       ═══════════════════════════════════════════════════════════════ */

    /** 只桩 selectById —— 失败路径不会走到 updateById，桩了会触发 Mockito 的 UnnecessaryStubbing */
    private void stubAuditTarget(String currentStatus) {
        when(postMapper.selectById(10L)).thenReturn(postWithStatus(currentStatus, 1L));
    }

    private void stubAuditUpdate() {
        when(postMapper.updateById(any())).thenReturn(1);
    }

    @Test
    @DisplayName("审核状态机 - 已发布帖不可再被驳回（409）")
    void auditPost_approvedPost_cannotBeReaudited() {
        stubAuditTarget("APPROVED");

        CulturalApiException ex = assertThrows(CulturalApiException.class,
                () -> postService.auditPost(10L, "REJECTED", "想撤下来"));

        assertEquals(409, ex.getCode());
        verify(postMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("审核状态机 - 已驳回帖不可再被通过（409）")
    void auditPost_rejectedPost_cannotBeReaudited() {
        stubAuditTarget("REJECTED");

        assertEquals(409, assertThrows(CulturalApiException.class,
                () -> postService.auditPost(10L, "APPROVED", null)).getCode());
        verify(postMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("审核状态机 - 目标状态不在白名单 → 400（且不触碰数据库）")
    void auditPost_unknownStatus_is400() {
        // 状态白名单校验发生在 selectById 之前，因此这里不需要任何桩
        for (String bad : new String[]{"PENDING", "DELETED", "approved", "", null}) {
            assertEquals(400, assertThrows(CulturalApiException.class,
                    () -> postService.auditPost(10L, bad, "x")).getCode(), "status=" + bad);
        }
        verify(postMapper, never()).selectById(any());
        verify(postMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("审核状态机 - 驳回必须填理由（与接口文档一致）")
    void auditPost_rejectWithoutReason_is400() {
        stubAuditTarget("PENDING");

        for (String blank : new String[]{null, "", "   "}) {
            assertEquals(400, assertThrows(CulturalApiException.class,
                    () -> postService.auditPost(10L, "REJECTED", blank)).getCode(), "reason=[" + blank + "]");
        }
        verify(postMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("审核状态机 - 通过时清空历史驳回理由")
    void auditPost_approveClearsRejectReason() {
        Post p = postWithStatus("PENDING", 1L);
        p.setRejectReason("上一次的驳回理由");
        when(postMapper.selectById(10L)).thenReturn(p);
        when(postMapper.updateById(any())).thenReturn(1);

        postService.auditPost(10L, "APPROVED", null);

        verify(postMapper).updateById(argThat(post ->
                "APPROVED".equals(post.getStatus()) && post.getRejectReason() == null));
    }

    @Test
    @DisplayName("审核状态机 - 驳回理由去除首尾空白后落库")
    void auditPost_rejectReasonIsTrimmed() {
        stubAuditTarget("PENDING");
        stubAuditUpdate();

        postService.auditPost(10L, "REJECTED", "  含营销话术  ");

        verify(postMapper).updateById(argThat(post -> "含营销话术".equals(post.getRejectReason())));
    }
}
