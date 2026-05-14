package com.zhiguan.gujian.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储工具
 */
@Slf4j
@Component
public class FileUtil {

    @Value("${zhiguan.assets.local-path:./assets}")
    private String localPath;

    /**
     * 保存上传文件到本地，返回访问URL路径
     */
    public String saveFile(MultipartFile file, String subDir) {
        try {
            String dir = localPath + File.separator + subDir;
            Files.createDirectories(Paths.get(dir));

            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".")) : ".bin";
            String fileName = UUID.randomUUID().toString() + ext;
            Path targetPath = Paths.get(dir, fileName);

            file.transferTo(targetPath.toFile());
            log.info("文件已保存: {}", targetPath);

            return "/assets/" + subDir + "/" + fileName;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 生成模拟的资产路径（用于异步任务演示）
     */
    public String generateMockAssetPath(String subDir, String ext) {
        String fileName = UUID.randomUUID().toString() + ext;
        return "/assets/" + subDir + "/" + fileName;
    }
}
