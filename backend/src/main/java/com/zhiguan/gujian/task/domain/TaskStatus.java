package com.zhiguan.gujian.task.domain;

/**
 * AI 任务状态机常量 — 对应 ai_task.status（PENDING/RUNNING/SUCCESS/FAILED）
 *
 * 注意：仅限任务链路使用；post 表的审核状态（PENDING/APPROVED/REJECTED）
 * 属于社区 BC 的状态机，与本类无关，不得混用。
 */
public final class TaskStatus {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private TaskStatus() {
    }
}
