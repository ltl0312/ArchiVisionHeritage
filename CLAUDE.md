# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

智观·古建 (ArchiVision Heritage) — 古建筑数字孪生与文化传承平台。核心能力：「一键幻筑」AI 3D 模型生成、「古建智析」VGGT 结构解析、「匠人社区」轻量级交流展示。

信息架构（V4 重构后，侧栏 5 个一级入口）：**营造志**（首页，平台能力宣言 + 精选档案）/ 古建智析 / 一键幻筑 / **我的档案** / **匠人社区**（原「文化社区」信息流迁入）。通知中心 / 个人设置 / 审核工作台是二级页面，入口在顶栏铃铛与账户菜单。

## 常用命令

### 后端 (Spring Boot 3.2.5, Java 17, Maven)

```bash
# 启动后端 (端口 8080)
cd backend && mvn spring-boot:run

# 运行所有测试
cd backend && mvn test

# 运行单个测试类
cd backend && mvn test -Dtest=ClassName

# 打包
cd backend && mvn clean package -DskipTests
```

### 前端 (Vue 3, Vite 5)

```bash
# 安装依赖
cd frontend && npm install

# 启动开发服务器 (端口 5173，自动代理 /api → localhost:8080)
cd frontend && npm run dev

# 生产构建
cd frontend && npm run build
```

### 数据库

```bash
# 初始化数据库（已创建则跳过）
mysql -u root -p < backend/src/main/resources/init.sql
```

预设账号：管理员 `admin` / `admin123`，演示用户 `demo_user` / `demo123`

## 架构总览

```
ArchiVisionHeritage/
├── backend/                    # Spring Boot 后端 (端口 8080)
│   └── src/main/java/com/zhiguan/gujian/
│       ├── analysis/           # 古建智析 BC（VGGT 转发）
│       ├── auth/               # 认证与用户 BC（JWT / 个人资料）
│       ├── community/          # 社区 BC（帖子 / 评论 / 点赞 / 关注）
│       ├── notification/       # 通知 BC（数字锦盒站内信）
│       ├── task/               # 幻筑任务 BC（异步状态机 + 幂等锁）
│       └── shared/             # 共享内核
│           ├── aop/            # @RateLimit 分布式限流切面
│           ├── common/         # Result / CulturalApiException / GlobalExceptionHandler
│           ├── config/         # Security / Redis / WebMVC / 线程池 / MyBatis-Plus
│           ├── security/       # JwtUtil (JJWT 0.12.5) + JwtAuthenticationFilter
│           ├── util/           # AncientDictUtil（文化词库增强）/ FileUtil
│           └── web/            # AdminController / UploadController
│       （每个 BC 内部分层：domain / application / infrastructure / interfaces）
├── frontend/                   # Vue 3 + Vite 前端 (端口 5173)
│   ├── preview/v4-preview.html # V4 高保真设计基准（7 页，非运行时产物）
│   ├── public/media/           # 随构建分发的静态影像
│   └── src/
│       ├── views/              # 页面组件 (按路由划分)
│       ├── components/         # ArchiveCard / PostCard / ImageUploader / Layout/*
│       ├── api/                # Axios 请求封装 (request.js 含拦截器)
│       ├── stores/             # Pinia 状态管理 (user, notification, theme)
│       ├── composables/        # useAsyncAction / useOptimisticLike
│       ├── utils/              # format / pagination / imageCompress
│       ├── router/             # Vue Router 配置
│       └── assets/style.css    # 设计系统唯一来源（令牌 + V4 原语）
├── vggt-api/                   # Python FastAPI VGGT 解析服务（端口 8000）
├── docs/                       # 计划书与 API 文档
└── prompt/                     # 项目需求文档与架构蓝图
```

## 核心技术机制

### 异步幻筑任务状态机 (TaskOrchestrationService)

流程：用户提交 Prompt → `ai_task` 表写入 PENDING → 返回 HTTP 202 → `@Async` 线程执行 → RUNNING → 模拟 3D 生成（3-7秒）→ SUCCESS → 写入 `model_asset`，并由 `TaskCompletedEventListener`（AFTER_COMMIT）写入 `notification`。

任务状态只有 `PENDING / RUNNING / SUCCESS / FAILED`，**没有子工序进度字段**——前端不得伪造百分比进度。

### Redis 幂等锁 (IdempotentLockService)

同一用户提交相同 Prompt 时，Lua 原子脚本拦截重复请求，返回已有 taskId。以 `userId + SHA256(prompt)` 为去重维度，TTL 10 分钟。

### 分布式限流 (RateLimitAspect)

`@RateLimit(maxCalls = 5)` 注解驱动，基于 Redis + Lua 原子计数。VGGT 解析接口每人每日限 5 次，次日自动清零。超限抛 `CulturalApiException`（温润文化文案）。

### JWT 鉴权

无状态 JWT + Spring Security。`/api/v1/auth/**`、`GET /api/v1/posts/**`、`/api/v1/analysis/zhixi/**` 公开；`/api/v1/admin/**` 需 ADMIN 角色；其余需登录。前端路由守卫从 JWT payload 解析 `role` 字段。

### VGGT 分析链路

前端上传图片 → `ZhiXiController` 通过 `RestTemplate` 转发 multipart/form-data 到 Python FastAPI (`127.0.0.1:8000/v1/analyze`) → **原样透传**其 JSON。

真实返回结构（前端按此渲染，勿臆造字段）：

```json
{
  "success": true,
  "analysis_id": "vggt_...",
  "image_info": { "filename": "...", "architectural_style": "唐代殿堂" },
  "structural_elements": [
    { "name": "斗栱", "bbox": [0.15, 0.30, 0.85, 0.55], "confidence": 0.94, "cultural_note": "..." }
  ],
  "summary": "..."
}
```

`bbox` 为**归一化**坐标（0–1）。Python 服务未启动时接口返回 **503**；`/zhixi/demos` 是已废弃的演示数据端点，前端不应再调用。

### 本地文件存储

`WebMvcConfig` 将 `/assets/**` 映射到本地磁盘 `./assets`（通过 `zhiguan.assets.local-path` 配置），生产环境建议 `/opt/zhiguan/assets/`。静态资源不经过 OSS。

### 前端设计系统（V4「玄墨 · 金线」）

- **默认深色「玄墨」**，浅色为「宣纸」；`themeStore` 通过 `documentElement[data-theme]` 驱动，并管理 `density-compact` 信息密度偏好。
- 三色语义：**琉璃金** `--color-accent` `#C9A227` = 行动；**矿物青** `--color-jade` = 技术过程；**丹砂** `--color-rose` = 文化标识。文字色另有 `--color-accent-text` / `--color-jade-text` / `--color-rose-text`（浅色下自动换深色以保证对比度）。
- 三层表面语义：`--color-bg-elev`（栏）/ `--color-surface`（卡片）/ `--color-surface-float`（浮层，**不透明**）。
- 全部令牌与 V4 原语（`.v4-panel` `.v4-card` `.v4-chip` `.v4-pill` `.v4-bench` `.v4-audit-row` `.v4-notif-row` …）**只在 `frontend/src/assets/style.css` 定义**。视图与组件**不得出现字面色值**（`#hex` / `rgba()`），影像上的叠加用 `.v4-scrim*` 或 `--color-scrim-*` / `--color-on-media*` / `--color-media-*` 令牌。
- **不使用 `backdrop-filter`**：内嵌 webview / 无 GPU 加速环境下它会退化为 CPU 逐帧高斯模糊；质感由底色不透明度 + 描边补偿。
- 视图根节点统一为 `<div class="view-shell">`（沉浸页加 `view-shell--flush`）；滚动容器是 `App.vue` 里的 `.view-host`，视图自己不要再套滚动层。
- 移动端规则**单一来源**：`style.css` 的 `@media (max-width: 900px)` 区块（不要再用 768px 断点）。
- 3D 渲染使用 `@google/model-viewer`，在 `ArchiveDetailView.vue` 中按路由**动态 import**（不进主包）。

## API 响应规范

所有接口返回统一结构 `{ code: 200, message: "success", data: {...} }`。前端 Axios 响应拦截器自动处理非 200 code 并弹出 `ElMessage.error`。

## 外部依赖

- **MySQL 8.0** — 端口 3306，数据库 `zhiguan_gujian`
- **Redis** — 端口 6379（幂等锁 + 限流计数）
- **Python VGGT API** — 端口 8000（古建结构分析引擎，可选，未启动时接口返回 503）
