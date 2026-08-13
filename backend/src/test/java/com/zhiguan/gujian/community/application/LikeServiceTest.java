package com.zhiguan.gujian.community.application;

import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.community.domain.LikeRecord;
import com.zhiguan.gujian.community.infrastructure.CommentMapper;
import com.zhiguan.gujian.community.infrastructure.LikeRecordMapper;
import com.zhiguan.gujian.community.infrastructure.PostMapper;
import com.zhiguan.gujian.community.interfaces.LikeRequest;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        likeService = new LikeServiceImpl(likeRecordMapper, postMapper,
                new PostBriefAssembler(userMapper, modelAssetMapper, likeRecordMapper, commentMapper));
    }

    @Test
    @DisplayName("点赞 - 首次点赞创建记录")
    void toggleLike_firstLike_createsRecord() {
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
}
