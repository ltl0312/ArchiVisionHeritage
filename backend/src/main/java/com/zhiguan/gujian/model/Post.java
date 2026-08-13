package com.zhiguan.gujian.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long modelAssetId;

    private String title;

    private String content;

    private String tags;

    private String coverImageUrl;

    /** 审核状态：PENDING（待审核）/ APPROVED（已发布）/ REJECTED（已驳回） */
    private String status;

    /** 驳回原因（仅 REJECTED 时有值） */
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
