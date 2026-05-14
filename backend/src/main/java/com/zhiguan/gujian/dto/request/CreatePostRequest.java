package com.zhiguan.gujian.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {
    @NotBlank @Size(max = 128)
    private String title;

    private String content;

    private Long modelAssetId;
}
