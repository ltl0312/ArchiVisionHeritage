package com.zhiguan.gujian.analysis.interfaces;

import com.zhiguan.gujian.analysis.application.AnalysisService;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import com.zhiguan.gujian.shared.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ZhiXiController 单元测试 — 薄控制器（转发 AnalysisService，RestTemplate 桩测试已下沉 AnalysisServiceTest）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VGGT 分析控制器测试")
class ZhiXiControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ZhiXiController zhiXiController;

    @Mock
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(zhiXiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("VGGT 分析成功 - 返回分析结果")
    void analyze_success_returnsResult() throws Exception {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("scene_id", "test_scene");
        mockResult.put("status", "success");

        when(analysisService.analyze(any())).thenReturn(mockResult);

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/v1/analysis/zhixi").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.scene_id").value("test_scene"));
    }

    @Test
    @DisplayName("VGGT 服务不可用 - 返回 503")
    void analyze_serviceUnavailable_returns503() throws Exception {
        when(analysisService.analyze(any()))
                .thenThrow(new CulturalApiException(503, "VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)"));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/v1/analysis/zhixi").file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)"));
    }

    @Test
    @DisplayName("获取演示数据列表")
    void listDemos_returnsList() throws Exception {
        when(analysisService.listDemos()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/analysis/zhixi/demos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("古建智能修复预留接口 - 业务码 501")
    void restorationPredict_returns501() throws Exception {
        // 501 占位为 Result.fail 直接返回（阶段二明示保留），非抛异常 → HTTP 200 + 业务码 501
        mockMvc.perform(post("/api/v1/analysis/restoration/predict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(501))
                .andExpect(jsonPath("$.message").value("古建智能修复功能即将上线，敬请期待"));
    }
}
