package com.zhiguan.gujian.annotation;

import java.lang.annotation.*;

/**
 * VGGT 接口防刷注解 — 默认每日5次
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 每日最大调用次数 */
    int maxCalls() default 5;
}
