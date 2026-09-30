package com.zhiguan.gujian.shared.aop;

import java.lang.annotation.*;

/**
 * 接口防刷注解 —— 基于 Redis + Lua 的每日计数限流。
 *
 * ⚠️ `key()` 必须按接口区分：计数器 key 由 `rate:<key>:<用户或IP>:<日期>` 组成，
 * 若两个接口用同一个 key，它们会**共用同一个计数器**
 * （原实现的 key 前缀被硬编码成 `rate:zhixi`，一旦把本注解加到别的接口上，
 *  那个接口就会消耗解析额度 —— 本注解新增 key 正是为了消除这个陷阱）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 每日最大调用次数 */
    int maxCalls() default 5;

    /** 计数维度标识，不同接口必须不同 */
    String key() default "default";

    /** 超限提示文案；留空则使用默认的古建文案 */
    String message() default "";
}
