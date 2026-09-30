package com.zhiguan.gujian.community.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LikeRequest {
    @NotNull
    private Long targetId;

    /**
     * 点赞对象类型。
     *
     * 必须限定为 POST / COMMENT —— 该值会直接写入 `like_record.target_type`
     * （DB 上是 ENUM），传非法值原先会撞 ENUM 约束并兜底成 **500**。
     */
    @NotBlank
    @Pattern(regexp = "POST|COMMENT", message = "targetType 必须为 POST 或 COMMENT")
    private String targetType;
}
