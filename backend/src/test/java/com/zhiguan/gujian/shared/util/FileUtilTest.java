package com.zhiguan.gujian.shared.util;

import com.zhiguan.gujian.shared.common.CulturalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传落盘的安全回归测试。
 *
 * 覆盖审计确认的两个漏洞（均已实测复现）：
 *   ① subDir 路径穿越 —— 原实现把用户输入直接拼进磁盘路径，可写到 assets 之外；
 *   ② 扩展名信任 —— 原实现从原始文件名取扩展名，HTML 伪装成 image/png 即落盘为 .html，
 *      再由 /assets/** 以 text/html 回读，构成同源存储型 XSS。
 */
class FileUtilTest {

    /** 合法的 1×1 PNG */
    private static final byte[] REAL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

    private static final String HTML = "<!doctype html><script>document.title=localStorage.getItem('token')</script>";

    @TempDir
    Path assetsRoot;

    private FileUtil fileUtil;

    @BeforeEach
    void setUp() {
        fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "localPath", assetsRoot.toString());
        ReflectionTestUtils.setField(fileUtil, "urlPrefix", "/assets");
    }

    private static MockMultipartFile file(String name, String contentType, byte[] body) {
        return new MockMultipartFile("file", name, contentType, body);
    }

    /* ═══════════ ① 路径穿越 ═══════════ */

    @Test
    @DisplayName("subDir 含 ../ 时拒绝，且不产生任何目录逃逸")
    void saveImage_rejectsPathTraversalSubDir() throws Exception {
        Path escapeTarget = assetsRoot.getParent().resolve("traversal-poc");
        List<String> malicious = List.of(
                "../traversal-poc",
                "../../traversal-poc",
                "..",
                "covers/../../escape",
                "covers/../..",
                "..\\windows-style",
                "/absolute",
                "a/b"
        );

        for (String subDir : malicious) {
            CulturalApiException ex = assertThrows(CulturalApiException.class,
                    () -> fileUtil.saveImage(file("ok.png", "image/png", REAL_PNG), subDir),
                    "subDir=" + subDir + " 应当被拒绝");
            assertEquals(400, ex.getCode(), "subDir=" + subDir);
        }

        assertFalse(Files.exists(escapeTarget), "不得在 assets 根之外创建目录");
        // assets 根下也不应残留任何被写入的文件
        try (Stream<Path> walk = Files.walk(assetsRoot)) {
            assertEquals(0, walk.filter(Files::isRegularFile).count(), "不应写入任何文件");
        }
    }

    @Test
    @DisplayName("subDir 为空 / null / 超长 / 含非法字符时拒绝")
    void saveImage_rejectsInvalidSubDir() {
        for (String subDir : new String[]{null, "", "   ", "a".repeat(33), "covers!", "封面", "cov ers"}) {
            assertThrows(CulturalApiException.class,
                    () -> fileUtil.saveImage(file("ok.png", "image/png", REAL_PNG), subDir),
                    "subDir=" + subDir + " 应当被拒绝");
        }
    }

    /* ═══════════ ② 扩展名信任 / 存储型 XSS ═══════════ */

    @Test
    @DisplayName("HTML 伪装成 image/png 时拒绝（原实现会落盘为 .html 并构成 XSS）")
    void saveImage_rejectsHtmlDisguisedAsImage() {
        CulturalApiException ex = assertThrows(CulturalApiException.class,
                () -> fileUtil.saveImage(file("poc.html", "image/png", HTML.getBytes(StandardCharsets.UTF_8)), "covers"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("图片"), "错误信息应说明内容不是图片");
    }

    @Test
    @DisplayName("SVG / PHP / 纯文本同样拒绝")
    void saveImage_rejectsOtherNonImagePayloads() {
        for (String payload : new String[]{"<svg xmlns=\"http://www.w3.org/2000/svg\"/>", "<?php echo 1;?>", "just text"}) {
            assertThrows(CulturalApiException.class,
                    () -> fileUtil.saveImage(file("x.png", "image/png", payload.getBytes(StandardCharsets.UTF_8)), "covers"),
                    "payload=" + payload);
        }
    }

    @Test
    @DisplayName("扩展名只由嗅探结果决定：真 PNG 即使命名为 .html、声明为 text/html，也存成 .png")
    void saveImage_extensionComesFromSniffedTypeNotFilename() {
        String url = fileUtil.saveImage(file("evil.html", "text/html", REAL_PNG), "covers");

        assertTrue(url.startsWith("/assets/covers/"), "url 应以配置的前缀与子目录开头，实际: " + url);
        assertTrue(url.endsWith(".png"), "扩展名应由嗅探结果给出，实际: " + url);
        assertFalse(url.contains(".html"), "不得沿用原始文件名的扩展名");
    }

    /* ═══════════ ③ 正常路径与配置 ═══════════ */

    @Test
    @DisplayName("合法图片落盘成功，文件确实存在且内容一致")
    void saveImage_writesFileWithSameContent() throws Exception {
        String url = fileUtil.saveImage(file("photo.jpg", "image/jpeg", jpegBytes()), "covers");

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path written = assetsRoot.resolve("covers").resolve(fileName);
        assertTrue(Files.exists(written), "文件应已落盘: " + written);
        assertEquals(jpegBytes().length, Files.size(written));
    }

    @Test
    @DisplayName("URL 前缀来自配置而非硬编码 /assets")
    void saveImage_usesConfiguredUrlPrefix() {
        ReflectionTestUtils.setField(fileUtil, "urlPrefix", "/static/uploads");
        String url = fileUtil.saveImage(file("a.png", "image/png", REAL_PNG), "images");
        assertTrue(url.startsWith("/static/uploads/images/"), "实际: " + url);
    }

    @Test
    @DisplayName("每次落盘使用随机 UUID 文件名，避免覆盖与枚举")
    void saveImage_generatesUniqueNames() {
        String a = fileUtil.saveImage(file("same.png", "image/png", REAL_PNG), "covers");
        String b = fileUtil.saveImage(file("same.png", "image/png", REAL_PNG), "covers");
        assertFalse(a.equals(b), "同名上传不应互相覆盖");
    }

    /** 最小 JPEG 头（FF D8 FF ...）—— 嗅探只需文件头 */
    private static byte[] jpegBytes() {
        byte[] bytes = new byte[64];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xE0;
        return bytes;
    }
}
