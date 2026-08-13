package com.zhiguan.gujian.service.impl;

import com.zhiguan.gujian.dto.response.TaskStatusResponse;
import com.zhiguan.gujian.mapper.AiTaskMapper;
import com.zhiguan.gujian.mapper.ModelAssetMapper;
import com.zhiguan.gujian.model.AiTask;
import com.zhiguan.gujian.model.ModelAsset;
import com.zhiguan.gujian.service.IdempotentLockService;
import com.zhiguan.gujian.service.NotificationService;
import com.zhiguan.gujian.service.TaskOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TaskOrchestrationServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务调度服务测试")
class TaskOrchestrationServiceImplTest {

    @InjectMocks
    private TaskOrchestrationServiceImpl taskService;

    @Mock
    private AiTaskMapper aiTaskMapper;

    @Mock
    private ModelAssetMapper modelAssetMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdempotentLockService idempotentLockService;

    @Mock
    private TaskAsyncExecutor taskAsyncExecutor;

    private AiTask testTask;

    @BeforeEach
    void setUp() {
        testTask = new AiTask();
        testTask.setId(1L);
        testTask.setUserId(1L);
        testTask.setTaskType("HUANZHU_3D");
        testTask.setOriginalPrompt("唐代大殿");
        testTask.setEnhancedPrompt("唐代风格大殿，恢弘大气...");
        testTask.setStatus("PENDING");
        testTask.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("提交新任务 - 成功创建")
    void submitHuanZhuTask_success() {
        when(idempotentLockService.tryAcquire(1L, "唐代大殿")).thenReturn(true);
        when(aiTaskMapper.insert(any(AiTask.class))).thenAnswer(invocation -> {
            AiTask task = invocation.getArgument(0);
            task.setId(1L); // 模拟 MyBatis Plus 自动生成 ID
            return 1;
        });

        TaskOrchestrationService.SubmitResult result = taskService.submitHuanZhuTask(1L, "唐代大殿");

        assertNotNull(result);
        assertNotNull(result.taskId());
        assertFalse(result.duplicate());
        verify(aiTaskMapper).insert(any());
        verify(taskAsyncExecutor).executeAsync(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("重复提交 - 幂等拦截返回已有任务")
    void submitHuanZhuTask_duplicate_returnsExisting() {
        when(idempotentLockService.tryAcquire(1L, "唐代大殿")).thenReturn(false);
        when(idempotentLockService.getExistingTaskId(1L, "唐代大殿")).thenReturn(1L);

        TaskOrchestrationService.SubmitResult result = taskService.submitHuanZhuTask(1L, "唐代大殿");

        assertNotNull(result);
        assertEquals(1L, result.taskId());
        assertTrue(result.duplicate());
        verify(aiTaskMapper, never()).insert(any());
    }

    @Test
    @DisplayName("获取任务状态 - 任务存在")
    void getTaskStatus_taskExists() {
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);

        TaskStatusResponse response = taskService.getTaskStatus(1L);

        assertNotNull(response);
        assertEquals(1L, response.getTaskId());
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    @DisplayName("获取任务状态 - 任务不存在返回 null")
    void getTaskStatus_taskNotFound_returnsNull() {
        when(aiTaskMapper.selectById(999L)).thenReturn(null);

        TaskStatusResponse response = taskService.getTaskStatus(999L);

        assertNull(response);
    }

    @Test
    @DisplayName("获取任务状态 - SUCCESS 状态包含资产信息")
    void getTaskStatus_success_includesAsset() {
        testTask.setStatus("SUCCESS");
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);

        ModelAsset asset = new ModelAsset();
        asset.setId(1L);
        asset.setTaskId(1L);
        asset.setPreview2dPath("/assets/preview.png");
        asset.setGlb3dPath("/assets/model.glb");

        when(modelAssetMapper.selectOne(any())).thenReturn(asset);

        TaskStatusResponse response = taskService.getTaskStatus(1L);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getPreview2dPath());
        assertNotNull(response.getGlb3dPath());
    }
}
