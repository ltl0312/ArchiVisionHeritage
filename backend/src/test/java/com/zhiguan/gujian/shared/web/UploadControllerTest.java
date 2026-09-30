package com.zhiguan.gujian.shared.web;

import com.zhiguan.gujian.shared.common.GlobalExceptionHandler;
import com.zhiguan.gujian.shared.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上传接口的契约与安全回归测试。
 *
 * 这是审计中「零测试」的 5 个 Controller 之一 —— 它恰好也是本次修复的安全入口，
 * 因此必须把白名单与内容嗅探的行为钉死。
 */
class UploadControllerTest {

    private static final byte[] REAL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

    @TempDir
    Path assetsRoot;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FileUtil fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "localPath", assetsRoot.toString());
        ReflectionTestUtils.setField(fileUtil, "urlPrefix", "/assets");

        mockMvc = MockMvcBuilders.standaloneSetup(new UploadController(fileUtil))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MockMultipartFile part(String name, String contentType, byte[] body) {
        return new MockMultipartFile("file", name, contentType, body);
    }

    /* ═══════════ 白名单 ═══════════ */

    @Test
    @DisplayName("subDir 白名单之外的取值 → 400")
    void uploadImage_rejectsSubDirOutsideWhitelist() throws Exception {
        for (String subDir : new String[]{"evil", "../../etc", "covers/../..", "COVERS", "tmp", "covers/sub"}) {
            int status = mockMvc.perform(multipart("/api/v1/upload/image")
                            .file(part("a.png", "image/png", REAL_PNG))
                            .param("subDir", subDir))
                    .andReturn().getResponse().getStatus();
            assertEquals(400, status, "subDir=[" + subDir + "] 应被拒绝，实际 HTTP " + status);
        }
        try (Stream<Path> walk = Files.walk(assetsRoot)) {
            assertEquals(0, walk.filter(Files::isRegularFile).count(), "被拒请求不得写入任何文件");
        }
    }

    @Test
    @DisplayName("subDir 传空值时按「未提供」处理，回落到白名单内的默认目录 images（不是漏洞）")
    void uploadImage_emptySubDirFallsBackToDefault() throws Exception {
        // MockMvc 会把空值参数视为未提供 → @RequestParam(defaultValue="images") 生效。
        // 关键在于它落在白名单内，而不是把空串拼进路径。
        mockMvc.perform(multipart("/api/v1/upload/image")
                        .file(part("a.png", "image/png", REAL_PNG))
                        .param("subDir", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/assets/images/")));
    }

    @Test
    @DisplayName("三个允许的目录都能正常落盘")
    void uploadImage_acceptsWhitelistedSubDirs() throws Exception {
        for (String subDir : new String[]{"covers", "images", "avatars"}) {
            mockMvc.perform(multipart("/api/v1/upload/image")
                            .file(part("a.png", "image/png", REAL_PNG))
                            .param("subDir", subDir))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/assets/" + subDir + "/")))
                    .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.endsWith(".png")));
        }
    }

    /* ═══════════ 内容嗅探 ═══════════ */

    @Test
    @DisplayName("HTML 伪装成 image/png → 400（原实现会存成 .html 并造成同源 XSS）")
    void uploadImage_rejectsHtmlDisguisedAsPng() throws Exception {
        byte[] html = "<!doctype html><script>alert(document.cookie)</script>".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/v1/upload/image")
                        .file(part("poc.html", "image/png", html))
                        .param("subDir", "covers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("真 PNG 但声明为 text/html、命名为 .html → 仍按 PNG 存为 .png")
    void uploadImage_sniffsRealTypeRegardlessOfDeclaredType() throws Exception {
        mockMvc.perform(multipart("/api/v1/upload/image")
                        .file(part("evil.html", "text/html", REAL_PNG))
                        .param("subDir", "covers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.endsWith(".png")));
    }

    /* ═══════════ 基础校验 ═══════════ */

    @Test
    @DisplayName("空文件 → 400")
    void uploadImage_rejectsEmptyFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/upload/image")
                        .file(part("empty.png", "image/png", new byte[0]))
                        .param("subDir", "covers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("超过 5MB → 400")
    void uploadImage_rejectsOversizedFile() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(REAL_PNG, 0, big, 0, REAL_PNG.length);

        mockMvc.perform(multipart("/api/v1/upload/image")
                        .file(part("big.png", "image/png", big))
                        .param("subDir", "covers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("未指定 subDir 时使用默认值 images")
    void uploadImage_defaultsToImages() throws Exception {
        mockMvc.perform(multipart("/api/v1/upload/image")
                        .file(part("a.png", "image/png", REAL_PNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/assets/images/")));
    }
}
