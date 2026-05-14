package com.zhiguan.gujian.utils;

import com.zhiguan.gujian.constant.CulturalLexicon;

/**
 * 幻筑提示词文化增强工具
 */
public class PromptEnhancerUtil {

    public static String enhance(String originalPrompt) {
        return CulturalLexicon.enhancePrompt(originalPrompt);
    }
}
