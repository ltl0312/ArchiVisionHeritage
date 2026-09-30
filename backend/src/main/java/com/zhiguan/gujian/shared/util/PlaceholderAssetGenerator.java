package com.zhiguan.gujian.shared.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 幻筑占位资产生成器。
 *
 * 背景：`TaskExecutionService` 是**模拟**生成 —— 它把
 * `/assets/preview/huanzhu_{id}_preview.png` 与 `/assets/models/huanzhu_{id}_model.glb`
 * 写进 `model_asset` 表，却**从不创建文件**。结果是幻筑成功后：
 *   · 封面必然取不到（实测 500 / 修复后 404），前端只能显示"封面图未能加载"；
 *   · 三维查看器必然加载失败。
 *
 * 本类为该模拟流程补上**真实存在的占位文件**，使 DB 里的路径不再是谎话：
 *   · 封面：一张明确的占位图（深底 + 金色测绘框 + 简化的殿宇剪影，不写文字以避免容器缺中文字体）
 *   · 模型：一个**结构合法**的最小 glTF 2.0 二进制（空场景），使 model-viewer 能正常加载而非报错
 *
 * 真正的 AI 3D 接入时，应替换为远端返回的真实资产，并删除本类。
 */
@Slf4j
public final class PlaceholderAssetGenerator {

    private static final int PREVIEW_WIDTH = 1024;
    private static final int PREVIEW_HEIGHT = 640;

    /** 玄墨底 / 金线 —— 与前端设计体系一致，避免出现刺眼的默认色 */
    private static final Color BG = new Color(0x0C, 0x0B, 0x0F);
    private static final Color GRID = new Color(0x1C, 0x1A, 0x22);
    private static final Color GOLD = new Color(0xC9, 0xA2, 0x27);
    private static final Color GOLD_LIGHT = new Color(0xE2, 0xC7, 0x7A);
    private static final Color PAPER = new Color(0xED, 0xEA, 0xE3);

    private PlaceholderAssetGenerator() {
    }

    /**
     * 生成占位封面 PNG。
     *
     * @param target 目标文件路径（父目录会自动创建）
     */
    public static void writePlaceholderPreview(Path target) throws IOException {
        Files.createDirectories(target.getParent());

        BufferedImage image = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g.setColor(BG);
            g.fillRect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);

            // 界画测绘底纹
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            for (int x = 0; x < PREVIEW_WIDTH; x += 64) {
                g.drawLine(x, 0, x, PREVIEW_HEIGHT);
            }
            for (int y = 0; y < PREVIEW_HEIGHT; y += 64) {
                g.drawLine(0, y, PREVIEW_WIDTH, y);
            }

            // 四角金线（与前端三维视口的取景框语言一致）
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(2f));
            int m = 40, len = 48;
            g.drawLine(m, m, m + len, m);
            g.drawLine(m, m, m, m + len);
            g.drawLine(PREVIEW_WIDTH - m, m, PREVIEW_WIDTH - m - len, m);
            g.drawLine(PREVIEW_WIDTH - m, m, PREVIEW_WIDTH - m, m + len);
            g.drawLine(m, PREVIEW_HEIGHT - m, m + len, PREVIEW_HEIGHT - m);
            g.drawLine(m, PREVIEW_HEIGHT - m, m, PREVIEW_HEIGHT - m - len);
            g.drawLine(PREVIEW_WIDTH - m, PREVIEW_HEIGHT - m, PREVIEW_WIDTH - m - len, PREVIEW_HEIGHT - m);
            g.drawLine(PREVIEW_WIDTH - m, PREVIEW_HEIGHT - m, PREVIEW_WIDTH - m, PREVIEW_HEIGHT - m - len);

            // 简化的殿宇剪影：台基 / 立柱 / 檐口 / 屋顶 —— 抽象示意，非渲染结果
            int cx = PREVIEW_WIDTH / 2;
            int baseY = PREVIEW_HEIGHT - 150;
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g.setColor(GOLD_LIGHT);
            g.drawLine(cx - 300, baseY, cx + 300, baseY);                    // 台基
            g.drawLine(cx - 260, baseY - 90, cx + 260, baseY - 90);          // 檐口
            g.drawLine(cx - 220, baseY, cx - 220, baseY - 90);               // 左柱
            g.drawLine(cx + 220, baseY, cx + 220, baseY - 90);               // 右柱
            g.drawLine(cx - 120, baseY, cx - 120, baseY - 90);
            g.drawLine(cx + 120, baseY, cx + 120, baseY - 90);

            // 屋顶：举折曲线的两段折线近似
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - 300, baseY - 90, cx - 120, baseY - 210);
            g.drawLine(cx - 120, baseY - 210, cx, baseY - 230);
            g.drawLine(cx, baseY - 230, cx + 120, baseY - 210);
            g.drawLine(cx + 120, baseY - 210, cx + 300, baseY - 90);

            // 底部一条淡金基线，暗示"资产已入库"
            g.setColor(new Color(0xC9, 0xA2, 0x27, 90));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(m, PREVIEW_HEIGHT - m, PREVIEW_WIDTH - m, PREVIEW_HEIGHT - m);

            // 右上角三枚小方块：占位标记（不写字，避免容器缺中文字体而显示豆腐块）
            g.setColor(PAPER);
            for (int i = 0; i < 3; i++) {
                g.fillRect(PREVIEW_WIDTH - m - 12 - i * 22, m + 10, 10, 10);
            }
        } finally {
            g.dispose();
        }

        ImageIO.write(image, "png", target.toFile());
    }

    /**
     * 生成结构合法的最小 glTF 2.0 二进制（GLB）占位文件。
     *
     * 只有一个 JSON chunk（GLB 规范允许无 BIN chunk），内容为空场景。
     * 这样 `<model-viewer>` 能正常加载并渲染空场景，而不是 404 / 解析失败。
     *
     * @param target 目标文件路径（父目录会自动创建）
     */
    public static void writePlaceholderGlb(Path target) throws IOException {
        Files.createDirectories(target.getParent());

        String json = "{\"asset\":{\"version\":\"2.0\","
                + "\"generator\":\"ArchiVision Heritage placeholder asset\"},"
                + "\"scenes\":[{\"nodes\":[]}],\"scene\":0}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // JSON chunk 必须 4 字节对齐，不足用空格(0x20)补齐
        int padding = (4 - (jsonBytes.length % 4)) % 4;
        int jsonChunkLength = jsonBytes.length + padding;
        int totalLength = 12 + 8 + jsonChunkLength;

        ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x46546C67);          // magic "glTF"
        buffer.putInt(2);                   // version
        buffer.putInt(totalLength);         // total length
        buffer.putInt(jsonChunkLength);     // chunk 0 length
        buffer.putInt(0x4E4F534A);          // chunk 0 type "JSON"
        buffer.put(jsonBytes);
        for (int i = 0; i < padding; i++) {
            buffer.put((byte) 0x20);
        }

        Files.write(target, buffer.array());
    }

    /**
     * 同时生成封面与模型占位文件，并记录日志。
     *
     * @param previewTarget 封面 PNG 的磁盘路径
     * @param modelTarget   GLB 的磁盘路径
     */
    public static void writePlaceholderAssets(Path previewTarget, Path modelTarget) throws IOException {
        writePlaceholderPreview(previewTarget);
        writePlaceholderGlb(modelTarget);
        log.info("幻筑占位资产已落盘: {} / {}", previewTarget, modelTarget);
    }
}
