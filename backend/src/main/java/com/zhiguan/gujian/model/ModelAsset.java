package com.zhiguan.gujian.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("model_asset")
public class ModelAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String preview2dPath;

    private String glb3dPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
