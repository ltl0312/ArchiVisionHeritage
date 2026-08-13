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

    /** 显式映射：字段名含数字边界，MyBatis-Plus 自动转换生成 preview2d_path，与表列 preview_2d_path 不一致 */
    @TableField("preview_2d_path")
    private String preview2dPath;

    @TableField("glb_3d_path")
    private String glb3dPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
