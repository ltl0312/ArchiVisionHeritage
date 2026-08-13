package com.zhiguan.gujian.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AncientDictUtil 单元测试
 */
@DisplayName("古建文化词库增强工具测试")
class AncientDictUtilTest {

    @Test
    @DisplayName("输入 null 返回默认增强 Prompt")
    void enhance_nullInput_returnsDefault() {
        String result = AncientDictUtil.enhance(null);
        assertNotNull(result);
        assertTrue(result.contains("中国古建筑"));
        assertTrue(result.contains("3D模型"));
    }

    @Test
    @DisplayName("输入空字符串返回默认增强 Prompt")
    void enhance_emptyInput_returnsDefault() {
        String result = AncientDictUtil.enhance("");
        assertNotNull(result);
        assertTrue(result.contains("中国古建筑"));
    }

    @Test
    @DisplayName("输入空白字符串返回默认增强 Prompt")
    void enhance_blankInput_returnsDefault() {
        String result = AncientDictUtil.enhance("   ");
        assertNotNull(result);
        assertTrue(result.contains("中国古建筑"));
    }

    @Test
    @DisplayName("匹配建筑类型 - 大殿")
    void enhance_buildingType_palace() {
        String result = AncientDictUtil.enhance("大殿");
        assertTrue(result.contains("面阔七间或九间"));
        assertTrue(result.contains("大殿"));
    }

    @Test
    @DisplayName("匹配建筑类型 - 亭")
    void enhance_buildingType_pavilion() {
        String result = AncientDictUtil.enhance("亭");
        assertTrue(result.contains("平面正方或六角八角"));
        assertTrue(result.contains("攒尖顶"));
    }

    @Test
    @DisplayName("匹配建筑类型 - 塔")
    void enhance_buildingType_pagoda() {
        String result = AncientDictUtil.enhance("塔");
        assertTrue(result.contains("楼阁式或密檐式"));
    }

    @Test
    @DisplayName("匹配朝代 - 唐")
    void enhance_dynasty_tang() {
        String result = AncientDictUtil.enhance("唐代");
        assertTrue(result.contains("唐代风格"));
        assertTrue(result.contains("恢弘大气"));
    }

    @Test
    @DisplayName("匹配朝代 - 宋")
    void enhance_dynasty_song() {
        String result = AncientDictUtil.enhance("宋代");
        assertTrue(result.contains("宋代风格"));
        assertTrue(result.contains("秀丽精巧"));
    }

    @Test
    @DisplayName("匹配朝代 - 明")
    void enhance_dynasty_ming() {
        String result = AncientDictUtil.enhance("明代");
        assertTrue(result.contains("明代风格"));
        assertTrue(result.contains("规整严谨"));
    }

    @Test
    @DisplayName("匹配朝代 - 清")
    void enhance_dynasty_qing() {
        String result = AncientDictUtil.enhance("清代");
        assertTrue(result.contains("清代风格"));
        assertTrue(result.contains("繁缛华丽"));
    }

    @Test
    @DisplayName("未指定朝代时默认添加唐代风格")
    void enhance_noDynasty_defaultTang() {
        String result = AncientDictUtil.enhance("大殿");
        assertTrue(result.contains("唐代风格"));
    }

    @Test
    @DisplayName("匹配屋顶形式 - 庑殿顶")
    void enhance_roof_wudian() {
        String result = AncientDictUtil.enhance("庑殿顶");
        assertTrue(result.contains("最高等级屋顶"));
    }

    @Test
    @DisplayName("匹配屋顶形式 - 歇山顶")
    void enhance_roof_xieshan() {
        String result = AncientDictUtil.enhance("歇山顶");
        assertTrue(result.contains("九脊殿"));
    }

    @Test
    @DisplayName("匹配色彩 - 朱红")
    void enhance_color_zhuhong() {
        String result = AncientDictUtil.enhance("朱红");
        assertTrue(result.contains("朱红色立柱与门窗"));
    }

    @Test
    @DisplayName("匹配色彩 - 黄琉璃瓦")
    void enhance_color_huangliuliwa() {
        String result = AncientDictUtil.enhance("黄琉璃瓦");
        assertTrue(result.contains("黄色琉璃瓦覆顶"));
    }

    @Test
    @DisplayName("多关键词组合匹配")
    void enhance_multipleKeywords() {
        String result = AncientDictUtil.enhance("唐代大殿");
        assertTrue(result.contains("唐代风格"));
        assertTrue(result.contains("面阔七间或九间"));
        assertTrue(result.contains("3D模型"));
    }

    @Test
    @DisplayName("增强结果包含通用质量标签")
    void enhance_containsQualityTags() {
        String result = AncientDictUtil.enhance("任意输入");
        assertTrue(result.contains("3D模型"));
        assertTrue(result.contains("高精度"));
        assertTrue(result.contains("PBR材质"));
        assertTrue(result.contains("古建筑"));
    }

    @Test
    @DisplayName("大殿类建筑默认推断重檐庑殿顶")
    void enhance_palace_defaultRoof() {
        String result = AncientDictUtil.enhance("大殿");
        assertTrue(result.contains("重檐庑殿顶") || result.contains("重檐歇山顶"));
    }

    @Test
    @DisplayName("园林类建筑默认配色")
    void enhance_garden_defaultColor() {
        String result = AncientDictUtil.enhance("园林");
        assertTrue(result.contains("白墙黑瓦") || result.contains("青砖黛瓦"));
    }
}
