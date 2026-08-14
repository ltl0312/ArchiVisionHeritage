package com.zhiguan.gujian.notification.application;

import com.zhiguan.gujian.notification.domain.Notification;
import com.zhiguan.gujian.notification.infrastructure.NotificationMapper;
import com.zhiguan.gujian.task.domain.event.TaskCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 幻筑任务完成事件监听器 — 事务提交后（AFTER_COMMIT）创建「数字锦盒」站内信（P0-2 修复）
 *
 * TaskExecutionService 在事务内发布 TaskCompletedEvent：
 * - 事务提交成功 → 本监听器被调用（单条 INSERT，无事务上下文时自动提交）
 * - 事务回滚 → 事件被丢弃，通知不产生（与资产回滚一致）
 * 事件发布方恒在事务内，无需 fallbackExecution。
 * 本类使 NotificationMapper 的使用收敛于通知 BC 内部，消除 TaskAsyncExecutor 的跨 BC 直插。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCompletedEventListener {

    /** 数字锦盒送达文案（与历史版本保持逐字一致） */
    public static final String HUANZHU_COMPLETED_MESSAGE = "您的古建数字锦盒已送达，请拆阅";

    private final NotificationMapper notificationMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setTaskId(event.taskId());
        notification.setMessage(HUANZHU_COMPLETED_MESSAGE);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
        log.info("幻筑任务 {} 完成，站内信已送达用户 {}", event.taskId(), event.userId());
    }
}
