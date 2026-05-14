package com.zhiguan.gujian.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 任务类型：HUANZHU_3D / ZHIXI_VGGT */
    private String taskType;

    private String originalPrompt;

    private String enhancedPrompt;

    /** 状态：PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
