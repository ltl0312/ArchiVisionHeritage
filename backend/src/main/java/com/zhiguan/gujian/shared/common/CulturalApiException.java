package com.zhiguan.gujian.shared.common;

/**
 * 业务异常 — 如 VGGT 次数耗尽、AI 任务失败等
 */
public class CulturalApiException extends RuntimeException {

    private final int code;

    public CulturalApiException(String message) {
        super(message);
        this.code = 429;
    }

    public CulturalApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
