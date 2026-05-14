package com.zhiguan.gujian.aop.aspect;

import com.zhiguan.gujian.annotation.RateLimit;
import com.zhiguan.gujian.exception.CulturalApiException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VGGT每日5次限制切面逻辑
 */
@Aspect
@Component
public class RateLimitAspect {

    private final Map<String, Integer> counter = new ConcurrentHashMap<>();
    private volatile LocalDate currentDate = LocalDate.now();

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 日期翻篇时重置计数器
        if (!LocalDate.now().equals(currentDate)) {
            synchronized (this) {
                if (!LocalDate.now().equals(currentDate)) {
                    counter.clear();
                    currentDate = LocalDate.now();
                }
            }
        }

        String key = "vggt_zhixi";
        int count = counter.getOrDefault(key, 0);

        if (count >= rateLimit.maxCalls()) {
            throw new CulturalApiException("VGGT 深度结构解析每日限调用 " + rateLimit.maxCalls() + " 次，今日额度已用完，请明日再试。");
        }

        counter.put(key, count + 1);
        return joinPoint.proceed();
    }
}
