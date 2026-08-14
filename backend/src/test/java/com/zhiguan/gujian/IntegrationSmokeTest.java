package com.zhiguan.gujian;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiguan.gujian.auth.application.AuthService;
import com.zhiguan.gujian.auth.domain.User;
import com.zhiguan.gujian.auth.infrastructure.UserMapper;
import com.zhiguan.gujian.auth.interfaces.LoginRequest;
import com.zhiguan.gujian.auth.interfaces.RegisterRequest;
import com.zhiguan.gujian.notification.application.NotificationService;
import com.zhiguan.gujian.notification.application.TaskCompletedEventListener;
import com.zhiguan.gujian.notification.interfaces.NotificationResponse;
import com.zhiguan.gujian.task.application.TaskOrchestrationService;
import com.zhiguan.gujian.task.domain.TaskStatus;
import com.zhiguan.gujian.task.interfaces.TaskStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成冒烟测试 — BaseTest 复活（@SpringBootTest + H2 + 本地 Redis 6379）
 *
 * 全链路：注册 → 登录 → 提交幻筑 → 轮询至 SUCCESS → 锦盒站内信产生。
 * 覆盖：H2 schema 与 MP 映射、Redis Lua 幂等锁、afterCommit 异步编排、
 *      TaskExecutionService 单事务（RUNNING→资产+SUCCESS）、
 *      TaskCompletedEventListener AFTER_COMMIT 通知落库。
 *
 * 注意：
 * 1. 禁止 @Transactional — afterCommit + @Async 依赖真实提交。
 * 2. 依赖本地 Redis 6379（docker compose redis 或 CI services.redis）。
 * 3. Prompt 含 UUID — 每次运行幂等 Key 唯一，无需 FLUSHDB。
 */
@DisplayName("集成冒烟 — 登录→幻筑→轮询→通知 全链路")
class IntegrationSmokeTest extends BaseTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TaskOrchestrationService taskOrchestrationService;

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("注册-登录-幻筑提交-轮询成功-锦盒通知 端到端")
    void fullChain_registerSubmitPollNotify() throws InterruptedException {
        String username = "it_user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "it-pass-123";
        String prompt = "集成测试·唐代凉亭·" + UUID.randomUUID();

        // 1. 注册 → 登录（AuthService → UserMapper → JwtUtil 真实链路）
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setNickname("集成测试员");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        String token = authService.login(loginRequest);
        assertNotNull(token, "登录应返回 JWT");

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        assertNotNull(user, "注册用户应可查询");
        Long userId = user.getId();

        // 2. 提交幻筑（Redis Lua 幂等锁 + PENDING 落库 + afterCommit 触发异步）
        TaskOrchestrationService.SubmitResult submit =
                taskOrchestrationService.submitHuanZhuTask(userId, prompt);
        assertFalse(submit.duplicate());
        Long taskId = submit.taskId();
        assertNotNull(taskId);

        // 3. 轮询至 SUCCESS（0ms 模拟耗时，10s 兜底超时）
        TaskStatusResponse status = pollUntilSuccess(taskId);
        assertNotNull(status, "任务 " + taskId + " 未在 10s 内达到 SUCCESS");
        assertEquals(TaskStatus.SUCCESS, status.getStatus());
        assertNotNull(status.getAssetId(), "SUCCESS 状态应携带 model_asset");
        assertTrue(status.getPreview2dPath().contains("huanzhu_" + taskId));

        // 4. 锦盒站内信（AFTER_COMMIT 监听器，SUCCESS 可见时通知必然已提交）
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        boolean hasBrocade = notifications.stream()
                .anyMatch(n -> TaskCompletedEventListener.HUANZHU_COMPLETED_MESSAGE.equals(n.getMessage())
                        && taskId.equals(n.getTaskId()));
        assertTrue(hasBrocade, "应产生「数字锦盒」通知，实际: " + notifications);
    }

    private TaskStatusResponse pollUntilSuccess(Long taskId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        TaskStatusResponse last = null;
        while (System.currentTimeMillis() < deadline) {
            last = taskOrchestrationService.getTaskStatus(taskId);
            if (last != null && TaskStatus.SUCCESS.equals(last.getStatus())) {
                return last;
            }
            Thread.sleep(200);
        }
        return last;
    }
}
