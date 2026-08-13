package com.zhiguan.gujian.analysis.interfaces;

import com.zhiguan.gujian.analysis.application.AnalysisService;
import com.zhiguan.gujian.analysis.domain.AnalysisDemo;
import com.zhiguan.gujian.shared.aop.RateLimit;
import com.zhiguan.gujian.shared.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 古建智析 VGGT — 结构分析（薄控制器：参数接收 + 限流 + 转发 Service）
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class ZhiXiController {

    private final AnalysisService analysisService;

    /**
     * VGGT 结构分析 — 转发图片至 Python 端进行真实解析
     * 保留每日5次防刷限制
     */
    @PostMapping("/zhixi")
    @RateLimit(maxCalls = 5)
    public Result<Map<String, Object>> analyze(@RequestParam("image") MultipartFile image) {
        return Result.ok(analysisService.analyze(image));
    }

    /** 获取智析演示数据列表（V1.0 过渡保留） */
    @GetMapping("/zhixi/demos")
    public Result<List<AnalysisDemo>> listDemos() {
        return Result.ok(analysisService.listDemos());
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
