package com.zhiguan.gujian.task.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HuanZhuRequest {

    /**
     * 建筑描述。
     *
     * 上限 512 与 `ai_task.original_prompt VARCHAR(512)` 对齐 ——
     * 原先只有 `@NotBlank`，超长输入会撞 DB 约束并兜底成 **500**（已实测：20000 字 → 500）。
     * 前端有 200 字限制，但接口不能依赖前端约束。
     */
    @NotBlank(message = "请输入建筑描述")
    @Size(max = 512, message = "描述不能超过 512 字")
    private String prompt;
}
