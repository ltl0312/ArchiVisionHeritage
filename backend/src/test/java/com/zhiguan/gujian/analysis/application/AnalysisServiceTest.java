package com.zhiguan.gujian.analysis.application;

import com.zhiguan.gujian.analysis.infrastructure.AnalysisDemoMapper;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * AnalysisService 单元测试 — VGGT 转发逻辑（原 ZhiXiControllerTest 的 RestTemplate 桩测试下沉）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("智析应用服务测试")
class AnalysisServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AnalysisDemoMapper analysisDemoMapper;

    @InjectMocks
    private AnalysisService analysisService;   // @Value 字段不注入，URL 为 null，桩需用 nullable 匹配

    private MockMultipartFile buildFile() {
        return new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());
    }

    @Test
    @DisplayName("VGGT 分析成功 - 返回结果 Map")
    void analyze_success_returnsMap() {
        when(restTemplate.exchange(nullable(String.class), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("scene_id", "test_scene"), HttpStatus.OK));

        Map<String, Object> result = analysisService.analyze(buildFile());

        assertNotNull(result);
        assertEquals("test_scene", result.get("scene_id"));
    }

    @Test
    @DisplayName("VGGT 连接失败 - 抛出 503 降级异常")
    void analyze_transportError_throws503() {
        when(restTemplate.exchange(nullable(String.class), any(), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        CulturalApiException e = assertThrows(
                CulturalApiException.class,
                () -> analysisService.analyze(buildFile())
        );

        assertEquals(503, e.getCode());
        assertEquals("VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)", e.getMessage());
    }

    @Test
    @DisplayName("VGGT 非 2xx 响应 - 抛出 503（现状 502 分支被 catch-all 吞掉，零行为变更）")
    void analyze_non2xx_throws503() {
        when(restTemplate.exchange(nullable(String.class), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_GATEWAY));

        CulturalApiException e = assertThrows(
                CulturalApiException.class,
                () -> analysisService.analyze(buildFile())
        );

        assertEquals(503, e.getCode());
    }
}
