package com.zhiguan.gujian.notification.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiguan.gujian.notification.interfaces.NotificationResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.notification.infrastructure.NotificationMapper;
import com.zhiguan.gujian.notification.domain.Notification;
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
import static org.mockito.Mockito.*;

/**
 * NotificationServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知服务测试")
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(1L);
        testNotification.setMessage("测试通知");
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取通知列表 - 逐条字段映射正确")
    void getNotifications_mapsFieldsForEachRecord() {
        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setUserId(1L);
        n2.setMessage("通知2");
        n2.setIsRead(true);
        n2.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(testNotification, n2));

        List<NotificationResponse> result = notificationService.getNotifications(1L);

        assertEquals(2, result.size());
        // 逐条核对映射（原实现是 assertNotNull + 一个"扫描自己刚构造的下标"的同义反复，恒真）
        NotificationResponse first = result.get(0);
        assertEquals(1L, first.getId());
        assertEquals("测试通知", first.getMessage());
        assertFalse(first.isRead(), "未读应映射为 read=false");

        NotificationResponse second = result.get(1);
        assertEquals(2L, second.getId());
        assertEquals("通知2", second.getMessage());
        assertTrue(second.isRead(), "已读应映射为 read=true");

        // 排序由 SQL 的 orderByDesc 负责，Mock 无法验证；
        // 真实排序由后端集成测试与 Docker E2E 覆盖（通知按 createdAt 倒序）
        verify(notificationMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取未读通知数量")
    void getUnreadCount() {
        when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        int count = notificationService.getUnreadCount(1L);

        assertEquals(3, count);
    }

    @Test
    @DisplayName("标记单条已读 - 通知属于当前用户")
    void markAsRead_belongsToUser_success() {
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);
        when(notificationMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> notificationService.markAsRead(1L, 1L));

        verify(notificationMapper).updateById(argThat(n -> Boolean.TRUE.equals(n.getIsRead())));
    }

    @Test
    @DisplayName("标记单条已读 - 通知不属于当前用户抛出 403")
    void markAsRead_notBelongsToUser_throwsException() {
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> notificationService.markAsRead(1L, 999L)
        );

        assertEquals(403, exception.getCode());
        assertEquals("无权操作此通知", exception.getMessage());
    }

    @Test
    @DisplayName("标记单条已读 - 通知不存在抛出 403")
    void markAsRead_notificationNotFound_throwsException() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        CulturalApiException exception = assertThrows(
                CulturalApiException.class,
                () -> notificationService.markAsRead(999L, 1L)
        );

        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("全部标记已读 - 发出一次批量更新")
    void markAllAsRead() {
        // 原实现只断言 assertNotNull(一个 @Mock) + 反射方法存在性 —— 恒真、零行为覆盖。
        // 这里至少把"确实发起了一次批量更新"钉住。
        // 说明：更新条件（按 userId 过滤）写在 LambdaUpdateWrapper 里，纯 Mockito 无法读取
        // （MyBatis-Plus 的 lambda 缓存需要 MP 上下文），该筛选由真实数据库的行为保证。
        when(notificationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(2);

        assertDoesNotThrow(() -> notificationService.markAllAsRead(1L));

        verify(notificationMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        // 不应退化成逐条更新
        verify(notificationMapper, never()).updateById(any());
    }
}
