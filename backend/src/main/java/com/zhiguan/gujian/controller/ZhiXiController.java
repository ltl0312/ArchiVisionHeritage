package com.zhiguan.gujian.controller;

import com.zhiguan.gujian.annotation.RateLimit;
import com.zhiguan.gujian.config.Result;
import com.zhiguan.gujian.mapper.AnalysisDemoMapper;
import com.zhiguan.gujian.model.AnalysisDemo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 古建智析 VGGT — 结构分析
 * 将图片转发至 Python VGGT API (FastAPI :8000) 进行真实结构推断
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class ZhiXiController {

    private final RestTemplate restTemplate;
    private final AnalysisDemoMapper analysisDemoMapper;

    /** Python VGGT API 地址（可通过 vggt.api.url 配置或 VGGT_API_URL 环境变量覆盖） */
    @Value("${vggt.api.url:http://127.0.0.1:8000/v1/analyze}")
    private String vggtApiUrl;

    /**
     * VGGT 结构分析 — 转发图片至 Python 端进行真实解析
     * 保留每日5次防刷限制
     */
    @PostMapping("/zhixi")
    @RateLimit(maxCalls = 5)
    public Result<Map<String, Object>> analyze(@RequestParam("image") MultipartFile image) {
        try {
            // 将 MultipartFile 封装为 RestTemplate 可发送的 multipart/form-data
            ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用 Python VGGT API 进行结构分析
            ResponseEntity<Map> response = restTemplate.exchange(
                    vggtApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return Result.ok(result);
            } else {
                return Result.fail(502, "VGGT 分析服务暂时不可用");
            }

        } catch (Exception e) {
            log.error("调用 VGGT API 失败", e);
            // Python 服务未启动或网络不通时的降级提示
            return Result.fail(503, "VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)");
        }
    }

    /** 获取智析演示数据列表（V1.0 过渡保留） */
    @GetMapping("/zhixi/demos")
    public Result<List<AnalysisDemo>> listDemos() {
        return Result.ok(analysisDemoMapper.selectList(null));
    }

    /**
     * 古建智能修复接口 — V1.2 预留
     * 为后续高阶古建智能修复能力铺路，当前返回 501 Not Implemented
     */
    @PostMapping("/restoration/predict")
    public Result<Void> restorationPredict() {
        return Result.fail(501, "古建智能修复功能即将上线，敬请期待");
    }
}
