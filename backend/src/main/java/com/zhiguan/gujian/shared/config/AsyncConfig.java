package com.zhiguan.gujian.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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

        // 拒绝策略：默认 AbortPolicy 会在队列满时直接抛 TaskRejectedException，
        // 而该异常抛在 TaskOrchestrationServiceImpl 的 afterCommit 回调里 ——
        // 事务已提交、任务已入库，却永远停在 PENDING，且幂等锁 10 分钟内不会释放。
        // CallerRunsPolicy 改为由提交线程执行，任务至少能被处理（背压而非丢弃）。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅停机：让在途的幻筑任务跑完，避免重启时任务被硬中断而停在 RUNNING。
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
