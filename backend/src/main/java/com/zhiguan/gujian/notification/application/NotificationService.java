package com.zhiguan.gujian.notification.application;

import com.zhiguan.gujian.notification.interfaces.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotifications(Long userId);

    int getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
