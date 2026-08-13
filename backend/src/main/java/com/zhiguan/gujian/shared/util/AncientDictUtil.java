package com.zhiguan.gujian.shared.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 古建文化词库增强工具 — 静态文化降维策略
 *
 * 核心思路：不引入 NLP 模型，依靠内置的"古建文化词库"对用户的简略 Prompt
 * 进行专业术语与风格描述的自动填充，为后续 AI 3D 生成提供高质量输入。
 *
 * 例如："唐代大殿" → "唐代风格大殿，单檐或重檐歇山顶，出檐深远，
 *                        斗栱宏大雄健，朱红立柱，木本色为主，恢弘大气"
 */
public class AncientDictUtil {

    /* ======================== 词库定义 ======================== */

    /** 建筑类型 → 结构特征 */
    private static final Map<String, String> BUILDING_TYPE_MAP = new LinkedHashMap<>();
    static {
        BUILDING_TYPE_MAP.put("大殿",  "面阔七间或九间，单檐或重檐歇山顶，台基高耸，前设月台");
        BUILDING_TYPE_MAP.put("殿堂",  "面阔开阔，进深宏敞，梁架规整，天花藻井精美");
        BUILDING_TYPE_MAP.put("亭",    "平面正方或六角八角，攒尖顶，飞檐翘角，四面通透");
        BUILDING_TYPE_MAP.put("塔",    "楼阁式或密檐式，逐层收分，檐角悬挂风铎，塔刹高耸");
        BUILDING_TYPE_MAP.put("桥",    "石拱桥或廊桥，券洞曲线优美，栏板望柱雕饰精致");
        BUILDING_TYPE_MAP.put("楼阁",  "二层以上，平座挑出，回廊环绕，登临可远眺");
        BUILDING_TYPE_MAP.put("牌坊",  "石质或木质，立柱冲天，额枋镌刻，斗栱层叠出檐");
        BUILDING_TYPE_MAP.put("寺庙",  "山门—天王殿—大雄宝殿中轴布局，钟鼓楼对峙，伽蓝七堂制");
        BUILDING_TYPE_MAP.put("园林",  "叠山理水，曲径通幽，亭台楼榭错落，借景框景成趣");
        BUILDING_TYPE_MAP.put("民居",  "四合院或天井式布局，硬山顶，青砖灰瓦，质朴内敛");
        BUILDING_TYPE_MAP.put("宫殿",  "前朝后寝格局，三重须弥座台基，黄琉璃瓦庑殿顶，金龙和玺彩画");
    }

    /** 朝代 → 风格特征 */
    private static final Map<String, String> DYNASTY_MAP = new LinkedHashMap<>();
    static {
        DYNASTY_MAP.put("唐", "唐代风格，恢弘大气，出檐深远，斗栱宏大雄健，鸱尾简洁有力，木本色为主");
        DYNASTY_MAP.put("宋", "宋代风格，秀丽精巧，举折平缓，装修精美，格子门窗，须弥座雕饰繁复");
        DYNASTY_MAP.put("明", "明代风格，规整严谨，色彩厚重，装饰简练，砖石工艺发达，琉璃技术成熟");
        DYNASTY_MAP.put("清", "清代风格，繁缛华丽，彩画绚烂，规制严格，和玺彩画与旋子彩画等级分明");
    }

    /** 屋顶形式 → 描述 */
    private static final Map<String, String> ROOF_MAP = new LinkedHashMap<>();
    static {
        ROOF_MAP.put("庑殿顶",   "最高等级屋顶，四面坡五脊，又称'四阿顶'，庄重威严");
        ROOF_MAP.put("歇山顶",   "九脊殿，两坡顶加周围廊，等级仅次于庑殿顶，造型丰富");
        ROOF_MAP.put("重檐庑殿顶", "双层庑殿顶，皇家宫殿最高规制，如故宫太和殿");
        ROOF_MAP.put("重檐歇山顶", "双层歇山顶，重要殿堂使用，如天安门城楼");
        ROOF_MAP.put("悬山顶",   "两坡顶，檩条挑出山墙之外，保护山墙不受雨淋");
        ROOF_MAP.put("硬山顶",   "两坡顶，山墙封顶不露檩，民间常用，防火防风");
        ROOF_MAP.put("攒尖顶",   "锥形屋顶，无正脊，多见于亭台楼阁与塔刹");
        ROOF_MAP.put("卷棚顶",   "弧形屋顶无正脊，柔美流畅，多用于园林建筑");
    }

    /** 色彩/材质 → 视觉描述 */
    private static final Map<String, String> COLOR_MAP = new LinkedHashMap<>();
    static {
        COLOR_MAP.put("朱红",     "朱红色立柱与门窗，象征生命力与礼制权威");
        COLOR_MAP.put("黄琉璃瓦",  "黄色琉璃瓦覆顶，皇家专用色，阳光下金碧辉煌");
        COLOR_MAP.put("青砖黛瓦",  "青灰色砖墙配深色瓦片，江南水乡韵味，素雅恬淡");
        COLOR_MAP.put("金碧辉煌",  "金色装饰与碧绿色彩绘交相辉映，富丽堂皇");
        COLOR_MAP.put("白墙黑瓦",  "徽派建筑经典配色，粉墙黛瓦马头墙，质朴典雅");
        COLOR_MAP.put("木本色",    "原木色泽，保留木材天然纹理，古朴温润");
    }

    /* ======================== 核心增强方法 ======================== */

    /**
     * 对用户输入的简略 Prompt 进行文化扩充增强。
     *
     * 匹配策略：逐层扫描用户输入中的关键词（建筑类型 → 朝代 → 屋顶 → 色彩），
     * 将匹配到的专业描述追加至原始 Prompt，形成适合 AI 3D 生成的高质量输入。
     *
     * @param originalPrompt 用户原始输入，如 "唐代大殿"
     * @return 增强后的 Prompt，如 "唐代风格大殿，恢弘大气，出檐深远..."
     */
    public static String enhance(String originalPrompt) {
        if (originalPrompt == null || originalPrompt.isBlank()) {
            return "中国古建筑，唐代风格大殿，重檐庑殿顶，斗栱宏大，出檐深远，朱红立柱，3D模型，高精度，PBR材质";
        }

        StringBuilder enhanced = new StringBuilder(originalPrompt.trim());

        // 第一层：匹配建筑类型，追加结构特征描述
        boolean hasBuildingType = false;
        for (Map.Entry<String, String> entry : BUILDING_TYPE_MAP.entrySet()) {
            if (originalPrompt.contains(entry.getKey())) {
                enhanced.append("，").append(entry.getValue());
                hasBuildingType = true;
                break; // 只匹配第一个建筑类型
            }
        }

        // 第二层：匹配朝代/风格，追加时代特征
        boolean hasDynasty = false;
        for (Map.Entry<String, String> entry : DYNASTY_MAP.entrySet()) {
            if (originalPrompt.contains(entry.getKey())) {
                enhanced.append("，").append(entry.getValue());
                hasDynasty = true;
                break;
            }
        }
        // 未指定朝代时默认唐代（唐代斗栱最宏大，3D 生成视觉效果最佳）
        if (!hasDynasty) {
            enhanced.append("，唐代风格，斗栱宏大，出檐深远，木本色为主");
        }

        // 第三层：匹配屋顶形式
        boolean hasRoof = false;
        for (Map.Entry<String, String> entry : ROOF_MAP.entrySet()) {
            if (originalPrompt.contains(entry.getKey())) {
                enhanced.append("，").append(entry.getValue());
                hasRoof = true;
                break;
            }
        }
        // 未指定屋顶时，根据建筑类型推断
        if (!hasRoof) {
            if (originalPrompt.contains("大殿") || originalPrompt.contains("殿堂")
                    || originalPrompt.contains("宫殿")) {
                enhanced.append("，重檐庑殿顶或重檐歇山顶，庄重威严");
            } else if (originalPrompt.contains("亭")) {
                enhanced.append("，攒尖顶，飞檐翘角");
            } else if (originalPrompt.contains("塔")) {
                enhanced.append("，攒尖顶塔刹");
            } else {
                enhanced.append("，歇山顶或悬山顶");
            }
        }

        // 第四层：匹配色彩描述
        boolean hasColor = false;
        for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
            if (originalPrompt.contains(entry.getKey())) {
                enhanced.append("，").append(entry.getValue());
                hasColor = true;
                break;
            }
        }
        if (!hasColor) {
            // 根据朝代推断配色
            if (originalPrompt.contains("宫殿") || originalPrompt.contains("大殿")) {
                enhanced.append("，朱红立柱，黄琉璃瓦覆顶，金碧辉煌");
            } else if (originalPrompt.contains("园林") || originalPrompt.contains("江南")) {
                enhanced.append("，白墙黑瓦，青砖黛瓦，素雅自然");
            } else {
                enhanced.append("，朱红立柱，青砖黛瓦，古朴典雅");
            }
        }

        // 收尾：3D 生成通用质量控制标签
        enhanced.append("，3D模型，高精度，PBR材质，古建筑");

        return enhanced.toString();
    }
}
