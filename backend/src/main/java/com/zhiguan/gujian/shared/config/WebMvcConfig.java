package com.zhiguan.gujian.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 配置本地文件系统静态资源映射，弃用OSS。
 * 请求 /assets/** 映射到本地磁盘路径（开发环境为 ./assets，生产环境建议 /opt/zhiguan/assets/）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${zhiguan.assets.local-path:./assets}")
    private String localPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(localPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);
    }
}
