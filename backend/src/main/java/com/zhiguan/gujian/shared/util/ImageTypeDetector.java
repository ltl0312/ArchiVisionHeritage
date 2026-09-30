package com.zhiguan.gujian.shared.util;

/**
 * 图片类型嗅探 —— 依据**文件魔数**判定真实类型，不信任客户端声明的 Content-Type。
 *
 * 为什么必须这样：原实现只校验 {@code MultipartFile.getContentType()}，而该值完全由
 * 客户端提供。把一段 HTML 声明成 {@code image/png} 上传，扩展名又取自原始文件名，
 * 就会在 {@code /assets/**}（permitAll）下以 {@code text/html} 被回读 ——
 * 构成同源存储型 XSS，可直接读取 localStorage 中的 JWT（已实测复现）。
 *
 * 因此：扩展名必须由嗅探结果决定，而不是由调用方或文件名决定。
 */
public final class ImageTypeDetector {

    private ImageTypeDetector() {
    }

    /** 支持的图片类型：扩展名与 MIME 都由嗅探结果给出，不由外部输入决定 */
    public enum ImageType {
        JPEG(".jpg", "image/jpeg"),
        PNG(".png", "image/png"),
        GIF(".gif", "image/gif"),
        WEBP(".webp", "image/webp");

        private final String extension;
        private final String mimeType;

        ImageType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String extension() {
            return extension;
        }

        public String mimeType() {
            return mimeType;
        }
    }

    /** 判定魔数所需的最小字节数（WebP 需要看到第 12 字节） */
    public static final int HEAD_BYTES = 12;

    /**
     * 依据文件头字节嗅探图片类型。
     *
     * @param head 文件起始字节（至少 {@link #HEAD_BYTES} 个，不足则返回 null）
     * @return 识别出的类型；无法识别（含空/过短）返回 null
     */
    public static ImageType detect(byte[] head) {
        if (head == null || head.length < HEAD_BYTES) {
            return null;
        }
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
            return ImageType.JPEG;
        }
        if (startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return ImageType.PNG;
        }
        if (startsWith(head, 'G', 'I', 'F', '8', '7', 'a') || startsWith(head, 'G', 'I', 'F', '8', '9', 'a')) {
            return ImageType.GIF;
        }
        // WebP: "RIFF" .... "WEBP"
        if (startsWith(head, 'R', 'I', 'F', 'F')
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return ImageType.WEBP;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != (prefix[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
