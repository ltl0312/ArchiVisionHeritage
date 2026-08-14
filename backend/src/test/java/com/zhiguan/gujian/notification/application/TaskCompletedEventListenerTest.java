package com.zhiguan.gujian.notification.application;

import com.zhiguan.gujian.notification.infrastructure.NotificationMapper;
import com.zhiguan.gujian.task.domain.event.TaskCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * TaskCompletedEventListener 单元测试 — 事件载荷 → 站内信字段映射
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务完成事件监听器测试")
class TaskCompletedEventListenerTest {

    @InjectMocks
    private TaskCompletedEventListener listener;

    @Mock
    private NotificationMapper notificationMapper;

    @Test
    @DisplayName("收到完成事件 - 插入 userId/taskId/文案/isRead=false 的站内信")
    void onTaskCompleted_insertsNotification() {
        listener.onTaskCompleted(new TaskCompletedEvent(1L, 1L));

        verify(notificationMapper).insert(argThat(n ->
                n.getUserId() == 1L
                        && n.getTaskId() == 1L
                        && "您的古建数字锦盒已送达，请拆阅".equals(n.getMessage())
                        && Boolean.FALSE.equals(n.getIsRead())));
    }
}
