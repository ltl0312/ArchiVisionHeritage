package com.zhiguan.gujian.task.application;

import com.zhiguan.gujian.task.domain.AiTask;
import com.zhiguan.gujian.task.infrastructure.AiTaskMapper;
import com.zhiguan.gujian.task.infrastructure.IdempotentLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TaskAsyncExecutor 单元测试 — 编排壳三分支：任务存在+成功 / 任务不存在 / 执行异常
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务异步执行器测试")
class TaskAsyncExecutorTest {

    @InjectMocks
    private TaskAsyncExecutor taskAsyncExecutor;

    @Mock
    private AiTaskMapper aiTaskMapper;

    @Mock
    private IdempotentLockService idempotentLockService;

    @Mock
    private TaskExecutionService taskExecutionService;

    private AiTask testTask;

    @BeforeEach
    void setUp() {
        testTask = new AiTask();
        testTask.setId(1L);
        testTask.setUserId(1L);
        testTask.setStatus("PENDING");
    }

    @Test
    @DisplayName("任务存在且执行成功 - 调用 execute + 释放幂等锁")
    void executeAsync_success_releasesLock() {
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);

        taskAsyncExecutor.executeAsync(1L, "唐代大殿", 1L);

        verify(taskExecutionService).execute(1L);
        verify(taskExecutionService, never()).markFailed(anyLong());
        verify(idempotentLockService).release(1L, "唐代大殿");
    }

    @Test
    @DisplayName("任务不存在 - 释放锁且不触发执行与失败标记")
    void executeAsync_taskNull_releasesLockOnly() {
        when(aiTaskMapper.selectById(999L)).thenReturn(null);

        taskAsyncExecutor.executeAsync(999L, "唐代大殿", 1L);

        verify(taskExecutionService, never()).execute(anyLong());
        verify(taskExecutionService, never()).markFailed(anyLong());
        verify(idempotentLockService).release(1L, "唐代大殿");
    }

    @Test
    @DisplayName("执行异常 - markFailed 兜底 + 释放幂等锁")
    void executeAsync_exception_marksFailedAndReleasesLock() {
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);
        doThrow(new RuntimeException("DB 故障")).when(taskExecutionService).execute(1L);

        taskAsyncExecutor.executeAsync(1L, "唐代大殿", 1L);

        verify(taskExecutionService).markFailed(1L);
        verify(idempotentLockService).release(1L, "唐代大殿");
    }
}
