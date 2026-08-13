package com.zhiguan.gujian.analysis.application;

import com.zhiguan.gujian.analysis.domain.AnalysisDemo;
import com.zhiguan.gujian.analysis.infrastructure.AnalysisDemoMapper;
import com.zhiguan.gujian.shared.common.CulturalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 古建智析 VGGT — 结构分析（原 ZhiXiController 逻辑整体下沉）
 * 将图片转发至 Python VGGT API (FastAPI :8000) 进行真实结构推断
 *
 * 注：现状 502 分支位于 try 内、被 catch-all 吞掉（非 2xx 实际返回 503），
 * 属既有行为，字节保持；若需 502 真实生效须将抛出移出 try（行为变更，另行处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final AnalysisDemoMapper analysisDemoMapper;

    /** Python VGGT API 地址（可通过 vggt.api.url 配置或 VGGT_API_URL 环境变量覆盖） */
    @Value("${vggt.api.url:http://127.0.0.1:8000/v1/analyze}")
    private String vggtApiUrl;

    /** VGGT 结构分析 — 转发图片至 Python 端进行真实解析（每日5次限流由 Controller 的 @RateLimit 负责） */
    public Map<String, Object> analyze(MultipartFile image) {
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
                return result;
            } else {
                throw new CulturalApiException(502, "VGGT 分析服务暂时不可用");
            }

        } catch (Exception e) {
            log.error("调用 VGGT API 失败", e);
            // Python 服务未启动或网络不通时的降级提示
            throw new CulturalApiException(503, "VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)");
        }
    }

    /** 获取智析演示数据列表（V1.0 过渡保留） */
    public List<AnalysisDemo> listDemos() {
        return analysisDemoMapper.selectList(null);
    }
}
