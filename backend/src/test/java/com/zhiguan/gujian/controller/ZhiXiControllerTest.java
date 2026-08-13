package com.zhiguan.gujian.controller;

import com.zhiguan.gujian.exception.GlobalExceptionHandler;
import com.zhiguan.gujian.mapper.AnalysisDemoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ZhiXiController 单元测试 — VGGT 分析接口
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VGGT 分析控制器测试")
class ZhiXiControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ZhiXiController zhiXiController;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AnalysisDemoMapper analysisDemoMapper;

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

        // @Value 字段在纯单测（@InjectMocks）下不注入，URL 为 null，桩需用 nullable 匹配
        when(restTemplate.exchange(nullable(String.class), any(), any(), eq(Map.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(mockResult, org.springframework.http.HttpStatus.OK));

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
        when(restTemplate.exchange(nullable(String.class), any(), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/v1/analysis/zhixi").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)"));
    }

    @Test
    @DisplayName("获取演示数据列表")
    void listDemos_returnsList() throws Exception {
        when(analysisDemoMapper.selectList(null)).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/analysis/zhixi/demos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
