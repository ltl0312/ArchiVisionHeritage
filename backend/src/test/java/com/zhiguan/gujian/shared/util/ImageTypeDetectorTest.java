package com.zhiguan.gujian.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 魔数嗅探的单元测试 —— 这是上传接口「不再信任客户端 Content-Type」的基础。
 */
class ImageTypeDetectorTest {

    private static byte[] head(int... bytes) {
        byte[] out = new byte[Math.max(bytes.length, ImageTypeDetector.HEAD_BYTES)];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = (byte) bytes[i];
        }
        return out;
    }

    @Test
    @DisplayName("JPEG：FF D8 FF 开头")
    void detect_jpeg() {
        assertEquals(ImageTypeDetector.ImageType.JPEG, ImageTypeDetector.detect(head(0xFF, 0xD8, 0xFF, 0xE0)));
    }

    @Test
    @DisplayName("PNG：89 50 4E 47 0D 0A 1A 0A 开头")
    void detect_png() {
        assertEquals(ImageTypeDetector.ImageType.PNG,
                ImageTypeDetector.detect(head(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)));
    }

    @Test
    @DisplayName("GIF：GIF87a / GIF89a")
    void detect_gif() {
        assertEquals(ImageTypeDetector.ImageType.GIF, ImageTypeDetector.detect("GIF87a______".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(ImageTypeDetector.ImageType.GIF, ImageTypeDetector.detect("GIF89a______".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @DisplayName("WebP：RIFF....WEBP")
    void detect_webp() {
        assertEquals(ImageTypeDetector.ImageType.WEBP, ImageTypeDetector.detect("RIFF____WEBP".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @DisplayName("HTML / 脚本内容一律识别为「不是图片」——这正是存储型 XSS 的入口")
    void detect_htmlIsNotAnImage() {
        assertNull(ImageTypeDetector.detect("<!doctype html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ImageTypeDetector.detect("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ImageTypeDetector.detect("<?php echo 1; ?>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ImageTypeDetector.detect("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("只有 RIFF 但不是 WEBP（如 WAV）不应误判")
    void detect_riffButNotWebp() {
        assertNull(ImageTypeDetector.detect("RIFF____WAVE".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @DisplayName("空、null、过短输入返回 null，不抛异常")
    void detect_degenerateInputs() {
        assertNull(ImageTypeDetector.detect(null));
        assertNull(ImageTypeDetector.detect(new byte[0]));
        assertNull(ImageTypeDetector.detect(new byte[]{(byte) 0x89, 0x50}));
    }

    @Test
    @DisplayName("扩展名与 MIME 由类型自身给出，不由外部输入决定")
    void typeCarriesOwnExtensionAndMime() {
        assertEquals(".jpg", ImageTypeDetector.ImageType.JPEG.extension());
        assertEquals(".png", ImageTypeDetector.ImageType.PNG.extension());
        assertEquals("image/jpeg", ImageTypeDetector.ImageType.JPEG.mimeType());
        assertEquals("image/webp", ImageTypeDetector.ImageType.WEBP.mimeType());
    }
}
