package com.zhiguan.gujian.controller;

import com.zhiguan.gujian.config.Result;
import com.zhiguan.gujian.exception.CulturalApiException;
import com.zhiguan.gujian.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 通用文件上传接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileUtil fileUtil;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    /**
     * 上传图片文件
     * 返回可访问的 URL 路径
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subDir", defaultValue = "images") String subDir) {

        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new CulturalApiException(400, "请选择要上传的文件");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CulturalApiException(400, "文件大小不能超过 5MB");
        }

        // 3. 校验文件类型
        String contentType = file.getContentType();
        boolean isAllowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            throw new CulturalApiException(400, "只支持 JPG, PNG, GIF, WebP 格式的图片");
        }

        // 4. 保存文件
        try {
            String url = fileUtil.saveFile(file, subDir);
            log.info("文件上传成功: {}", url);
            return Result.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new CulturalApiException(500, "文件上传失败，请稍后重试");
        }
    }
}
