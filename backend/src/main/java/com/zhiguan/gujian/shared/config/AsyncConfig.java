package com.zhiguan.gujian.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /** 幻筑异步线程池 — 参数外置 zhiguan.task.pool.*（P2-8），@Value 带默认值兼容缺失配置 */
    @Bean("taskExecutor")
    public Executor taskExecutor(@Value("${zhiguan.task.pool.core-size:4}") int corePoolSize,
                                 @Value("${zhiguan.task.pool.max-size:8}") int maxPoolSize,
                                 @Value("${zhiguan.task.pool.queue-capacity:100}") int queueCapacity,
                                 @Value("${zhiguan.task.pool.name-prefix:zhiguan-async-}") String namePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(namePrefix);
        executor.initialize();
        return executor;
    }
}
