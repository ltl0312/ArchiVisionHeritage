package com.zhiguan.gujian.community.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank @Size(max = 1024)
    private String content;

    private Long parentId;
}
