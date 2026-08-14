package com.zhiguan.gujian.task.application;

import com.zhiguan.gujian.task.domain.AiTask;
import com.zhiguan.gujian.task.domain.ModelAsset;
import com.zhiguan.gujian.task.domain.event.TaskCompletedEvent;
import com.zhiguan.gujian.task.infrastructure.AiTaskMapper;
import com.zhiguan.gujian.task.infrastructure.ModelAssetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TaskExecutionService 单元测试 — 单事务执行语义（P0-2）
 * 断言用字面量而非 TaskStatus 常量，钉死写入 DB 的真实字符串契约（同时反向验证常量类取值）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务执行服务测试（单事务）")
class TaskExecutionServiceTest {

    @InjectMocks
    private TaskExecutionService taskExecutionService;

    @Mock
    private AiTaskMapper aiTaskMapper;

    @Mock
    private ModelAssetMapper modelAssetMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private AiTask testTask;

    @BeforeEach
    void setUp() {
        // 纯 Mockito 环境不注入 @Value 字段，此处显式赋值；min==max==0 → sleep(0) 立即返回
        ReflectionTestUtils.setField(taskExecutionService, "simulateMinMs", 0L);
        ReflectionTestUtils.setField(taskExecutionService, "simulateMaxMs", 0L);
        ReflectionTestUtils.setField(taskExecutionService, "assetPreviewDir", "/assets/preview");
        ReflectionTestUtils.setField(taskExecutionService, "assetModelDir", "/assets/models");

        testTask = new AiTask();
        testTask.setId(1L);
        testTask.setUserId(1L);
        testTask.setTaskType("HUANZHU_3D");
        testTask.setStatus("PENDING");
    }

    @Test
    @DisplayName("成功路径 - RUNNING 更新 → 资产插入 → SUCCESS 更新 → 发布完成事件（顺序与载荷）")
    void execute_success_orderedOperations() {
        // 注意：服务复用了同一个 AiTask 对象（RUNNING→SUCCESS 原地变更），
        // argThat 在 verify 时读取的是对象当前状态（已变为 SUCCESS），
        // 故状态流转用 thenAnswer 在调用时刻捕获，避免可变对象陷阱。
        List<String> statuses = new ArrayList<>();
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);
        when(aiTaskMapper.updateById(any())).thenAnswer(invocation -> {
            statuses.add(((AiTask) invocation.getArgument(0)).getStatus());
            return 1;
        });
        when(modelAssetMapper.insert(any(ModelAsset.class))).thenAnswer(invocation -> {
            ModelAsset asset = invocation.getArgument(0);
            asset.setId(100L);
            return 1;
        });

        taskExecutionService.execute(1L);

        assertEquals(Arrays.asList("RUNNING", "SUCCESS"), statuses);
        // 资产对象与事件 record 为独立对象（不被后续变更），可用 argThat 断言
        InOrder inOrder = inOrder(modelAssetMapper, applicationEventPublisher);
        inOrder.verify(modelAssetMapper).insert(argThat(a ->
                a.getTaskId() == 1L
                        && "/assets/preview/huanzhu_1_preview.png".equals(a.getPreview2dPath())
                        && "/assets/models/huanzhu_1_model.glb".equals(a.getGlb3dPath())));
        // 显式指定匹配器类型：publishEvent 有 (Object)/(ApplicationEvent) 两个重载，
        // 若不指定类型会推断为 ApplicationEvent 导致 instanceof 不可转换
        inOrder.verify(applicationEventPublisher).publishEvent(argThat((TaskCompletedEvent ev) ->
                ev.taskId() == 1L && ev.userId() == 1L));
    }

    @Test
    @DisplayName("失败路径 - 资产插入抛异常 → 异常传播且不发布事件（回滚语义）")
    void execute_failure_noEventPublished() {
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);
        when(modelAssetMapper.insert(any(ModelAsset.class))).thenThrow(new RuntimeException("DB 故障"));

        assertThrows(RuntimeException.class, () -> taskExecutionService.execute(1L));

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("失败路径 - 任务不存在抛 IllegalStateException 且无任何写入")
    void execute_taskNotFound_throws() {
        when(aiTaskMapper.selectById(999L)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> taskExecutionService.execute(999L));

        verify(aiTaskMapper, never()).updateById(any());
        verify(modelAssetMapper, never()).insert(any());
    }

    @Test
    @DisplayName("markFailed - 写入 FAILED 状态与固定错误文案")
    void markFailed_writesFailedStatus() {
        when(aiTaskMapper.selectById(1L)).thenReturn(testTask);

        taskExecutionService.markFailed(1L);

        verify(aiTaskMapper).updateById(argThat(t ->
                "FAILED".equals(t.getStatus())
                        && "任务执行失败，请稍后重试".equals(t.getErrorMessage())));
    }
}
