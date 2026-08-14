package com.zhiguan.gujian.task.domain.event;

/**
 * 幻筑任务完成领域事件 — 仅成功时发布，携带通知所需的 taskId + userId（YAGNI：result 省略）
 *
 * 发布方：TaskExecutionService.execute()（事务内发布）
 * 消费方：notification/application/TaskCompletedEventListener（@TransactionalEventListener AFTER_COMMIT）
 */
public record TaskCompletedEvent(Long taskId, Long userId) {
}
