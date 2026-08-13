package com.zhiguan.gujian.task.interfaces;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HuanZhuRequest {
    @NotBlank(message = "请输入建筑描述")
    private String prompt;
}
