package com.zhiguan.gujian.shared.web;

import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.shared.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * 通用文件上传接口
 *
 * 安全说明（原实现的两个漏洞已实测复现并修复）：
 *  - {@code subDir} 改为**白名单**，不再接受任意字符串（原先 {@code ../../} 可写到 assets 之外）；
 *  - 文件类型改为**按魔数嗅探**（{@link FileUtil#saveImage}），不再信任客户端声明的
 *    Content-Type —— 原先把 HTML 声明成 image/png 即可落盘为 .html，构成同源存储型 XSS。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileUtil fileUtil;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /** 允许的存放子目录 —— 与前端实际使用的目录保持一致 */
    private static final Set<String> ALLOWED_SUB_DIRS = Set.of("covers", "images", "avatars");

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

        // 2. 校验文件大小（spring.servlet.multipart 亦有一层 5MB 限制 → 413）
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CulturalApiException(400, "文件大小不能超过 5MB");
        }

        // 3. 校验存放目录（白名单）—— 杜绝 subDir 路径穿越
        if (!ALLOWED_SUB_DIRS.contains(subDir)) {
            throw new CulturalApiException(400, "不支持的存放目录: " + subDir);
        }

        // 4. 按魔数嗅探真实类型后落盘（扩展名由嗅探结果决定，与文件名无关）
        String url = fileUtil.saveImage(file, subDir);
        return Result.ok(Map.of("url", url));
    }
}
