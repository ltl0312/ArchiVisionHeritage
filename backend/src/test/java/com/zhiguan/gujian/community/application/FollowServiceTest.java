package com.zhiguan.gujian.community.application;

import com.zhiguan.gujian.community.infrastructure.FollowRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FollowServiceImpl 单元测试 — 原 CommunityServiceImplTest 的 toggleFollow 部分
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("关注服务测试")
class FollowServiceTest {

    @Mock
    private FollowRecordMapper followRecordMapper;

    @InjectMocks
    private FollowServiceImpl followService;

    @Test
    @DisplayName("关注 - 首次关注创建记录")
    void toggleFollow_firstFollow_createsRecord() {
        when(followRecordMapper.selectOne(any())).thenReturn(null);
        when(followRecordMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> followService.toggleFollow(1L, 2L));

        verify(followRecordMapper).insert(any());
    }

    @Test
    @DisplayName("关注 - 不能关注自己")
    void toggleFollow_self_followIgnored() {
        assertDoesNotThrow(() -> followService.toggleFollow(1L, 1L));

        verify(followRecordMapper, never()).selectOne(any());
        verify(followRecordMapper, never()).insert(any());
    }
}
