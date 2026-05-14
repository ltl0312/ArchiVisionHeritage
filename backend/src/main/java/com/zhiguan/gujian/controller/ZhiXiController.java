package com.zhiguan.gujian.controller;

import com.zhiguan.gujian.annotation.RateLimit;
import com.zhiguan.gujian.config.Result;
import com.zhiguan.gujian.mapper.AnalysisDemoMapper;
import com.zhiguan.gujian.model.AnalysisDemo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 古建智析 VGGT — 结构分析（当前为离线演示模式）
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class ZhiXiController {

    private final AnalysisDemoMapper analysisDemoMapper;

    /** VGGT 结构分析 — 包含每日5次限制防刷 */
    @PostMapping("/zhixi")
    @RateLimit(maxCalls = 5)
    public Result<AnalysisDemo> analyze(@RequestParam("image") MultipartFile image) {
        // 当前返回离线演示数据
        List<AnalysisDemo> demos = analysisDemoMapper.selectList(null);
        AnalysisDemo demo = demos.isEmpty() ? null : demos.get(0);
        if (demo == null) {
            return Result.fail(404, "暂无演示数据");
        }
        return Result.ok(demo);
    }

    /** 获取智析演示数据列表 */
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
