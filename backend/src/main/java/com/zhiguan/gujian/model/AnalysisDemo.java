package com.zhiguan.gujian.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("analysis_demo")
public class AnalysisDemo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String mockJsonData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
