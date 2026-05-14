package com.zhiguan.gujian.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Long taskId;
    private String message;
    private boolean isRead;
    private String createdAt;
}
