# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

智观·古建 (ArchiVision Heritage) — 古建筑数字孪生与文化传承平台。三大核心功能：「一键幻筑」AI 3D 模型生成、「古建智析」VGGT 结构解析、「文化社区」轻量级交流展示。

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
│       ├── controller/         # RESTful API 控制器
│       ├── service/impl/       # 业务逻辑 + @Async 异步任务调度
│       ├── mapper/             # MyBatis-Plus 数据访问接口
│       ├── model/              # 实体类 (User, Post, AiTask, ModelAsset 等)
│       ├── dto/request|response/  # 请求/响应 DTO
│       ├── config/             # Spring Security, Redis, WebMVC, 线程池配置
│       ├── security/           # JwtUtil (JJWT 0.12.5)
│       ├── filter/             # JwtAuthenticationFilter
│       ├── aop/aspect/         # @RateLimit 分布式限流切面
│       ├── annotation/         # 自定义注解 (@RateLimit)
│       ├── constant/           # TaskStatusEnum, CulturalLexicon
│       ├── exception/          # GlobalExceptionHandler + CulturalApiException
│       └── utils/              # AncientDictUtil, FileUtil, PromptEnhancerUtil
├── frontend/                   # Vue 3 + Vite 前端 (端口 5173)
│   └── src/
│       ├── views/              # 页面组件 (按路由划分)
│       ├── api/                # Axios 请求封装 (request.js 含拦截器)
│       ├── stores/             # Pinia 状态管理 (user, notification)
│       ├── router/             # Vue Router 配置
│       └── assets/style.css    # 全局设计系统 (CSS 变量 + 日夜模式)
└── prompt/                     # 项目需求文档与架构蓝图
```

## 核心技术机制

### 异步幻筑任务状态机 (TaskOrchestrationService)

流程：用户提交 Prompt → `ai_task` 表写入 PENDING → 返回 HTTP 202 → `@Async` 线程执行 → RUNNING → 模拟 3D 生成（3-7秒）→ SUCCESS → 写入 `model_asset` + `notification`。

### Redis 幂等锁 (IdempotentLockService)

同一用户提交相同 Prompt 时，Lua 原子脚本拦截重复请求，返回已有 taskId。以 `userId + SHA256(prompt)` 为去重维度，TTL 10 分钟。

### 分布式限流 (RateLimitAspect)

`@RateLimit(maxCalls = 5)` 注解驱动，基于 Redis + Lua 原子计数。VGGT 解析接口每人每日限 5 次，次日自动清零。

### JWT 鉴权

无状态 JWT + Spring Security。`/api/v1/auth/**`、`GET /api/v1/posts/**`、`/api/v1/analysis/zhixi/**` 公开；`/api/v1/admin/**` 需 ADMIN 角色；其余需登录。前端路由守卫从 JWT payload 解析 `role` 字段。

### VGGT 分析链路

前端上传图片 → `ZhiXiController` 通过 `RestTemplate` 转发 multipart/form-data 到 Python FastAPI (`127.0.0.1:8000/v1/analyze`) → 降级返回 `analysis_demo` 表中的模拟 JSON 数据。

### 本地文件存储

`WebMvcConfig` 将 `/assets/**` 映射到本地磁盘 `./assets`（通过 `zhiguan.assets.local-path` 配置），生产环境建议 `/opt/zhiguan/assets/`。静态资源不经过 OSS。

### 前端设计系统

CSS 变量定义在 `:root`，支持 `[data-theme="dark"]` 夜间模式。主色调琥珀金 `#D97706`，点缀色月季红 `#E11D48`，背景暖宣纸色。3D 模型渲染使用 `@google/model-viewer` 组件。

## API 响应规范

所有接口返回统一结构 `{ code: 200, message: "success", data: {...} }`。前端 Axios 响应拦截器自动处理非 200 code 并弹出 `ElMessage.error`。

## 外部依赖

- **MySQL 8.0** — 端口 3306，数据库 `zhiguan_gujian`
- **Redis** — 端口 6379（幂等锁 + 限流计数）
- **Python VGGT API** — 端口 8000（古建结构分析引擎，可选，未启动时接口返回 503）
