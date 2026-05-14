package com.zhiguan.gujian.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequest {
    @NotNull
    private Long targetId;

    @NotBlank
    private String targetType; // POST or COMMENT
}
