package com.zhiguan.gujian.constant;

import java.util.Map;

/**
 * 古建文化词库 — 用于一键幻筑的Prompt优化
 */
public class CulturalLexicon {

    public static final Map<String, String> DYNASTY_MAP = Map.of(
        "唐", "唐代风格，恢弘大气，出檐深远，斗栱雄大",
        "宋", "宋代风格，秀丽精巧，举折平缓，装修精美",
        "明", "明代风格，规整严谨，色彩厚重，装饰简练",
        "清", "清代风格，繁缛华丽，彩画绚烂，规制严格"
    );

    public static final Map<String, String> ROOF_MAP = Map.of(
        "庑殿顶", "最高等级屋顶，四面坡，庄重威严",
        "歇山顶", "九脊殿，等级仅次于庑殿顶，造型丰富",
        "悬山顶", "两坡顶，屋檐挑出山墙之外",
        "硬山顶", "两坡顶，山墙封顶，民间常用",
        "攒尖顶", "锥形屋顶，常用于亭台楼阁",
        "重檐庑殿顶", "双层庑殿顶，皇家宫殿最高规制",
        "重檐歇山顶", "双层歇山顶，重要殿堂使用"
    );

    public static final Map<String, String> COLOR_MAP = Map.of(
        "朱红", "朱红色立柱，象征生命力与权威",
        "黄琉璃瓦", "黄色琉璃瓦，皇家专用色，尊贵辉煌",
        "青砖黛瓦", "青灰色砖墙配深色瓦片，江南水乡韵味",
        "金碧辉煌", "金色装饰与碧绿色彩绘，富丽堂皇",
        "白墙黑瓦", "徽派建筑经典配色，素雅质朴"
    );

    /**
     * 对用户原始Prompt进行文化扩充
     */
    public static String enhancePrompt(String originalPrompt) {
        StringBuilder enhanced = new StringBuilder(originalPrompt);
        enhanced.append("，中国古建筑");

        // 检测用户是否已包含朝代信息
        boolean hasDynasty = DYNASTY_MAP.keySet().stream().anyMatch(originalPrompt::contains);
        if (!hasDynasty) {
            enhanced.append("，唐代风格，斗栱宏大，出檐深远");
        }

        boolean hasRoof = ROOF_MAP.keySet().stream().anyMatch(originalPrompt::contains);
        if (!hasRoof) {
            enhanced.append("，重檐庑殿顶");
        }

        boolean hasColor = COLOR_MAP.keySet().stream().anyMatch(originalPrompt::contains);
        if (!hasColor) {
            enhanced.append("，朱红立柱，黄琉璃瓦");
        }

        enhanced.append("，3D模型，高精度，PBR材质");
        return enhanced.toString();
    }
}
