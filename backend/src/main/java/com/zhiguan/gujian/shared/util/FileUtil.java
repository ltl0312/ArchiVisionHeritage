package com.zhiguan.gujian.shared.util;

import com.zhiguan.gujian.shared.common.CulturalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地文件存储工具（图片上传）。
 *
 * 安全约束（原实现两处漏洞，已实测复现，此处为修复）：
 *  1. **路径穿越**：原实现把用户传入的 subDir 直接拼进磁盘路径，
 *     {@code subDir=../../traversal-poc} 可把文件写到 assets 目录之外（进程以 root 运行）。
 *     现在 subDir 必须匹配 {@link #SAFE_SUB_DIR}，且控制器另有白名单 —— 双层防护。
 *  2. **扩展名信任**：原实现从原始文件名取扩展名，于是"HTML 伪装成 image/png"
 *     会落盘为 .html，再由 /assets/** 以 text/html 回读，构成同源存储型 XSS。
 *     现在扩展名**只由 {@link ImageTypeDetector} 的魔数嗅探结果决定**。
 */
@Slf4j
@Component
public class FileUtil {

    /** 允许的目录名：仅字母数字下划线连字符，1–32 位。拒绝分隔符与 ".." */
    private static final Pattern SAFE_SUB_DIR = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    @Value("${zhiguan.assets.local-path:./assets}")
    private String localPath;

    /** URL 前缀由配置提供，不再硬编码 "/assets" */
    @Value("${zhiguan.assets.url-prefix:/assets}")
    private String urlPrefix;

    /**
     * 保存上传的图片，返回可访问的 URL 路径。
     *
     * @param file   上传文件（调用方已校验非空与大小上限）
     * @param subDir 存放子目录（须为安全目录名）
     * @return 形如 {@code <url-prefix>/<subDir>/<uuid>.<ext>} 的访问路径
     */
    public String saveImage(MultipartFile file, String subDir) {
        String safeSubDir = sanitizeSubDir(subDir);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new CulturalApiException(500, "文件读取失败，请重试");
        }

        byte[] head = content.length > ImageTypeDetector.HEAD_BYTES
                ? java.util.Arrays.copyOf(content, ImageTypeDetector.HEAD_BYTES)
                : content;
        ImageTypeDetector.ImageType type = ImageTypeDetector.detect(head);
        if (type == null) {
            // 注意：这里拒绝的是"内容不是图片"，而不是"声明的 Content-Type 不对"
            throw new CulturalApiException(400, "文件内容不是可识别的图片（仅支持 JPG / PNG / GIF / WebP）");
        }

        // 扩展名只来自嗅探结果，与原始文件名、客户端声明的 MIME 无关
        String fileName = UUID.randomUUID() + type.extension();
        try {
            Path dir = Paths.get(localPath, safeSubDir).toAbsolutePath().normalize();
            Path target = dir.resolve(fileName).normalize();
            // 兜底断言：目标必须仍在 assets 根之内
            if (!target.startsWith(Paths.get(localPath).toAbsolutePath().normalize())) {
                throw new CulturalApiException(400, "非法的存放路径");
            }
            Files.createDirectories(dir);
            Files.write(target, content);
            log.info("图片已保存: {} ({}, {} bytes)", target, type.mimeType(), content.length);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new CulturalApiException(500, "文件保存失败，请稍后重试");
        }

        return urlPrefix + "/" + safeSubDir + "/" + fileName;
    }

    /** 校验目录名：必须匹配白名单字符集，杜绝 ".." 与路径分隔符 */
    private String sanitizeSubDir(String subDir) {
        if (subDir == null || !SAFE_SUB_DIR.matcher(subDir).matches()) {
            throw new CulturalApiException(400, "非法的存放目录");
        }
        return subDir;
    }
}
