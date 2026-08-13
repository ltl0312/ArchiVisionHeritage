package com.zhiguan.gujian.controller;

import com.zhiguan.gujian.config.Result;
import com.zhiguan.gujian.dto.response.NotificationResponse;
import com.zhiguan.gujian.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 获取通知列表 */
    @GetMapping
    public Result<List<NotificationResponse>> getNotifications(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(notificationService.getNotifications(userId));
    }

    /** 获取未读通知数量（用于红点显示） */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
}
