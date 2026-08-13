package com.zhiguan.gujian.task.interfaces;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskStatusResponse {
    private Long taskId;
    private String status;
    private Long assetId;
    private String preview2dPath;
    private String glb3dPath;
    private String errorMessage;
}
