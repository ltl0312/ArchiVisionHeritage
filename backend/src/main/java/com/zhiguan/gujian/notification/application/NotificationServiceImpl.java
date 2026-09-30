package com.zhiguan.gujian.notification.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiguan.gujian.notification.interfaces.NotificationResponse;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.notification.infrastructure.NotificationMapper;
import com.zhiguan.gujian.notification.domain.Notification;
import com.zhiguan.gujian.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 单次返回的通知上限。
     *
     * 原实现无任何上限，会把用户的全部通知一次性查出来并序列化 ——
     * 通知是只增不减的表，长期使用后响应体积无界。
     *
     * 这里先做**硬上限**（按时间倒序取最近 N 条）作为止血，而不是直接改成 Page：
     * 分页会改变响应结构（数组 → {records,total,...}），需要同步改
     * `frontend/src/api/notification.js` 与通知中心视图，属于跨前后端的契约变更，
     * 已列入待办（见 .workbuddy/_backend-fix-plan.md 的残留项）。
     */
    private static final int MAX_NOTIFICATIONS = 200;

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
                        // 常量拼接，无注入风险（MyBatis-Plus 未提供 limit 的链式 API）
                        .last("LIMIT " + MAX_NOTIFICATIONS));

        if (list.size() == MAX_NOTIFICATIONS) {
            log.warn("用户 {} 的通知达到单次返回上限 {}，更早的通知未返回（待接入分页）",
                    userId, MAX_NOTIFICATIONS);
        }

        return list.stream().map(n -> NotificationResponse.builder()
                .id(n.getId())
                .taskId(n.getTaskId())
                .message(n.getMessage())
                .isRead(Boolean.TRUE.equals(n.getIsRead()))
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "")
                .build()).collect(Collectors.toList());
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)).intValue();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new CulturalApiException(403, "无权操作此通知");
        }
        notification.setIsRead(true);
        notificationMapper.updateById(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .set(Notification::getIsRead, true));
    }
}
