# 智观·古建 (ArchiVision Heritage)

> 以技术为舟，载文化远航 — 文化遗产数字化与智能创作平台

**智观·古建**是一个以古建筑文化遗产为核心的数字孪生与智能创作平台。平台集成了 VGGT 视觉引擎进行古建结构深度解析，支持 AI 3D 模型生成（一键幻筑），并构建了一个轻量、沉浸的古建文化社区。前端采用大厂级 UI/UX 设计规范与中国传统色彩体系，致力于让每一块瓦当、每一组斗栱在数字世界重获新生。

---

## 目录结构

```
ArchiVisionHeritage/
├── backend/                          # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/zhiguan/gujian/
│       │   ├── ZhiguanApplication.java      # 启动类
│       │   ├── auth/                         # 认证 BC（登录/注册/个人中心）
│       │   │   ├── application/  domain/  infrastructure/  interfaces/
│       │   ├── community/                    # 社区 BC（帖子/评论/点赞/关注）
│       │   ├── task/                         # 幻筑 BC（任务状态机/资产/幂等锁/领域事件）
│       │   ├── analysis/                     # 智析 BC（VGGT 结构分析）
│       │   ├── notification/                 # 通知 BC（站内信/数字锦盒/事件监听器）
│       │   └── shared/                       # 共享层（aop/config/security/common/util/web）
│       └── main/resources/
│           ├── application.yml / -docker.yml / -prod.yml
│           ├── logback-spring.xml           # 日志滚动策略
│           ├── init.sql                     # MySQL 初始化脚本（含查询索引）
│           └── scripts/                     # Redis Lua 脚本（限流/幂等锁）
│
├── frontend/                         # Vue 3 前端
│   ├── vite.config.js                # Vite 配置 (含 /api 代理)
│   ├── index.html
│   ├── preview/v4-preview.html       # V4 高保真设计基准（7 个页面，非运行时产物）
│   ├── public/media/                 # 随构建分发的静态影像（首屏主视觉）
│   └── src/
│       ├── main.js                    # 入口
│       ├── App.vue                    # 根组件（侧栏 + 顶栏 + 视图宿主）
│       ├── router/index.js            # 路由配置 (含守卫)
│       ├── assets/style.css           # 设计系统唯一来源（令牌 + V4 原语）
│       ├── stores/                    # Pinia 状态管理
│       │   ├── user.js                # 用户状态
│       │   ├── notification.js        # 通知状态
│       │   └── theme.js               # 视觉偏好（主题 + 信息密度）
│       ├── api/                       # Axios 接口封装
│       │   ├── request.js             # Axios 实例 (含拦截器)
│       │   ├── auth.js                # 认证接口
│       │   ├── community.js           # 社区接口
│       │   ├── huanzhu.js             # 幻筑接口
│       │   ├── zhixi.js               # 智析接口
│       │   ├── notification.js        # 通知接口
│       │   ├── upload.js              # 图片上传接口
│       │   └── admin.js               # 管理接口
│       ├── utils/
│       │   ├── format.js              # 标签解析
│       │   ├── pagination.js          # 分页兜底
│       │   └── imageCompress.js       # 上传前 canvas 压缩
│       ├── composables/               # 复用逻辑（loading / 乐观点赞）
│       ├── components/
│       │   ├── ArchiveCard.vue        # 档案卡（营造志 / 我的档案共用）
│       │   ├── PostCard.vue           # 社区信息流卡片
│       │   ├── ImageUploader.vue      # 上传组件（含上传前压缩）
│       │   ├── CommentList.vue
│       │   └── Layout/                # SideNav / TopBar / UserMenu / BrocadeDialog
│       └── views/                     # 页面组件
│           ├── HeritageFeedView.vue    # 营造志（首页 · /home）
│           ├── CommunitySquareView.vue # 匠人社区（/community）
│           ├── PostDetailView.vue      # 帖子详情（/community/post/:id）
│           ├── ArchiveDetailView.vue   # 数字档案沉浸详情（/archive/:id，含 3D 查看器）
│           ├── UserProfileView.vue     # 我的档案（/archive）
│           ├── ZhiXiView.vue           # 古建智析（/zhixi，三区工作台）
│           ├── HuanZhuView.vue         # 一键幻筑（/huanzhu，创作区 + 工序时间轴）
│           ├── NotificationListView.vue # 通知中心（/notifications）
│           ├── SettingsView.vue        # 个人设置（/settings）
│           ├── AdminDashboardView.vue  # 审核工作台（/admin）
│           └── LoginView.vue           # 登录 / 注册
│
├── docs/
│   ├── 整体UI重构计划书.md             # V4 UI 重构计划（含差距矩阵与技术债清单）
│   └── API接口文档.md                 # 完整 API 文档
├── prompt/
│   └── 古建平台前端UI优化方案.md       # UI 设计规范文档
└── 项目进程.md                        # 项目开发进程记录
```

---

## 核心功能

### 营造志 (Heritage Feed — 首页 `/home`)
- **能力宣言首屏**：说明平台是什么、凭什么可信；技术事实条列出引擎 / 渲染管线 / 后端栈
- **双能力卡**：古建智析与一键幻筑的直接入口
- **精选数字档案**：取社区已公开档案的前 3 件，「查看全部」跳匠人社区
- 首页**不再是内容流** —— 内容流已迁至「匠人社区」，两条线各司其职

### 匠人社区 (Community Square — `/community`)
- **信息流**：卡片式帖子流 + 分页，按已加载页做前端关键词筛选
- **帖子发布**：Quill 富文本 + 图片上传（上传前自动压缩），内容审核机制
- **社交互动**：点赞（含弹跳动画）、评论、关注

### 我的档案 (My Archive — `/archive`)
- **我的档案**：本人发布的全部档案，按真实 `status` 筛选（全部 / 已发布 / 待审核 / 已驳回）
- **我的收藏**：本人点赞过的档案

### 古建智析 (ZhiXi — VGGT)
- **三区工作台**：投放区 / 三维视口 / 构件与文化解读
- **结构解析**：上传古建图片，VGGT 引擎返回构件定位（归一化 bbox）、置信度与文化解读
- **过程可见**：视口直接叠加真实构件框选与热区标签，点选构件切换文化解读
- **限流保护**：单用户每日 5 次调用节制，429 响应 + 温润文化提示（页内呈现，不只是一闪而过的 toast）

### 一键幻筑 (HuanZhu — AI 3D)
- **文字生成 3D**：自然语言描述古建，AI 生成精美三维模型
- **文化要素标签**：按朝代 / 屋顶形制 / 色彩分类的 chip，点击即真实增删提示词
- **异步任务**：提交后轮询状态，工序时间轴展示真实任务状态与已等待秒数
- **数字锦盒**：生成完成弹窗通知

### 管理后台 (Admin)
- **审核工作台**：待审队列 + 侧栏规则卡，逐条通过 / 驳回；驳回强制填理由
- **RBAC 权限**：USER / ADMIN 角色隔离

---

## 技术栈

| 层 | 技术 |
|---|---|
| 前端框架 | Vue 3 (Composition API + `<script setup>`) |
| 构建工具 | Vite 5 |
| UI 组件库 | Element Plus 2.7 |
| 状态管理 | Pinia |
| HTTP 客户端 | Axios (拦截器 + JWT) |
| 设计系统 | V4「玄墨 · 金线」— 令牌 + `.v4-*` 原语，单一来源 `assets/style.css` |
| 3D 渲染 | Google model-viewer 4.x (glTF/GLB)，按路由动态 import |
| 后端框架 | Spring Boot 3.2 |
| ORM | MyBatis-Plus 3.5 |
| 安全 | Spring Security + JWT (jjwt 0.12) |
| 数据库 | MySQL 8 |
| 并发 | Spring Async + 自定义 @RateLimit 注解 |
| 外部服务 | VGGT Python API (:8000)、AI 3D API |

---

## 快速启动

### 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Redis 7+**

### 1. 数据库初始化

```bash
mysql -u root -p < backend/src/main/resources/init.sql
```

初始化脚本会：
- 创建 `zhiguan_gujian` 数据库
- 建立全部 9 张业务表（user、post、comment、like_record、follow_record、ai_task、model_asset、notification、analysis_demo）
- 创建管理员账号：`admin` / `admin123`
- 创建演示用户：`demo_user` / `demo123`
- 导入 VGGT 演示解析数据

### 2. 启动后端

```bash
# 首次：复制环境变量样例（compose 已无明文默认密码，必须提供 DB_PASSWORD）
cp .env.example .env   # 填入实际 DB_PASSWORD 后：
cd backend
# 数据库密码通过环境变量注入（Git Bash: export DB_PASSWORD=xxx；CMD: set DB_PASSWORD=xxx）
mvn spring-boot:run
```

后端启动于：`http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev      # 开发模式
npm run build    # 生产构建
```

前端启动于：`http://localhost:5173`

---

## 测试

```bash
cd backend
mvn test        # 单元测试 + 集成冒烟（111 个用例）
mvn verify      # 全量构建门禁（CI 同款）
```

- 单元测试：Mockito standalone，无需外部依赖。
- 集成冒烟（`IntegrationSmokeTest`）：@SpringBootTest + H2（`schema-h2.sql`）+ 本地 Redis **6379 需运行**（`docker compose up -d redis`，或 CI `services.redis`）。
- 前端：`npm run build`（无 lint/test 脚本）。

开发环境下，Vite 自动将 `/api` 请求代理至 `http://localhost:8080`。

### 4. (可选) 启动 VGGT Python 服务

古建智析功能需要 Python VGGT API 服务在 `http://127.0.0.1:8000` 运行。如未启动，调用智析接口会返回 503 降级提示。演示数据功能不受影响。

---

## 设计规范

### 设计体系 V4「玄墨 · 金线」Dark Curatorial

默认模式为**玄墨深色**（路演投影对比度更高；点云 / 深度图 / 线框等技术素材在深底上才成立），
浅色「宣纸」模式用于长时间阅读文献。三色语义各守其职、互不越界：

| 令牌 | 语义 | 深色（默认） | 用途 |
|------|------|------|------|
| `--color-accent` | 琉璃金 | `#C9A227` | **唯一行动色**（按钮 / 选中 / 强调） |
| `--color-accent-text` | 金 · 文字 | `#E2C77A` | 金色文字（浅色下自动换深金保证对比度） |
| `--color-jade` | 矿物青 | `#4F9E93` | **技术过程**（置信度 / 引擎 / 日志） |
| `--color-rose` | 丹砂 | `#B4453A` | **文化标识**（朝代 / 文保 / 未读） |
| `--color-bg-base` | 玄墨 | `#0A0A0C` | 全局背景与三维视口底 |
| `--color-bg-elev` | 抬升层 | `#111014` | 栏层（侧栏 / 顶栏） |
| `--color-surface` | 卡片层 | `rgba(255,255,255,0.055)` | 内容卡片 / 面板 |
| `--color-surface-float` | 浮层实底 | `#1B1822` | 弹出菜单 / 弹窗 / 下拉（**不透明**） |

三层表面语义（栏 / 卡片 / 浮层）与全部 V4 组件原语（`.v4-panel` `.v4-chip` `.v4-pill` `.v4-card`
`.v4-bench` `.v4-audit-row` `.v4-notif-row` …）都定义在 `frontend/src/assets/style.css`，
**是设计系统的唯一来源**；视图与组件只允许引用令牌与原语，不得出现字面色值。

支持 `[data-theme="dark"]`（默认）与 `[data-theme="light"]`（宣纸）自动切换，
以及 `html.density-compact` 信息密度偏好（真实改变容器级 padding / gap）。

### 4pt 间距系统

所有外边距、内边距、图标尺寸均为 4px 的整数倍：

| 变量 | 像素 | 用途 |
|------|------|------|
| `--spacing-xs` | 4px | 图标与文字间距 |
| `--spacing-sm` | 8px | 头像与昵称间距 |
| `--spacing-md` | 16px | 卡片内边距 / 网格间距 |
| `--spacing-lg` | 24px | 模块内边距 |
| `--spacing-xl` | 32px | 页面边距（`.view-shell`） |
| `--spacing-xxl` | 64px | 大区块间距 |

### 中文排版

跨平台字体栈（正文无衬线 / 标题衬线）：
```
--font-family-base:   'Noto Sans SC', -apple-system, ..., "PingFang SC", "Microsoft YaHei", sans-serif
--font-family-serif:  'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', STSong, SimSun, serif
```

正文 14px / 行高 1.8；页面级标题 `--font-size-display` 34px；元信息 `--font-size-meta` 11px。

---

## API 文档

完整 API 文档参见：[docs/API接口文档.md](docs/API接口文档.md)

### 接口概览

| 模块 | 端点 | 说明 |
|------|------|------|
| 认证 | `POST /api/v1/auth/login` | 登录 |
| 认证 | `POST /api/v1/auth/register` | 注册 |
| 社区 | `GET /api/v1/posts` | 帖子流（仅 APPROVED，分页） |
| 社区 | `GET /api/v1/posts/{id}` | 帖子详情 |
| 社区 | `POST /api/v1/posts` | 发布帖子 |
| 社区 | `POST /api/v1/posts/{id}/comments` | 发表评论 |
| 社区 | `POST /api/v1/interactions/like` | 点赞/取消 |
| 社区 | `POST /api/v1/users/{id}/follow` | 关注/取消 |
| 个人 | `GET/PUT /api/v1/users/me` | 个人信息 |
| 幻筑 | `POST /api/v1/tasks/huanzhu` | 提交 3D 生成 |
| 幻筑 | `GET /api/v1/tasks/{id}/status` | 轮询任务 |
| 智析 | `POST /api/v1/analysis/zhixi` | VGGT 解析 |
| 通知 | `GET /api/v1/notifications` | 通知列表 |
| 管理 | `GET /api/v1/admin/posts/pending` | 待审核列表 |
| 管理 | `PUT /api/v1/admin/posts/{id}/audit` | 审核操作 |

---

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 跨域方案

- **开发环境**：Vite proxy (`/api` → `localhost:8080`)
- **生产部署**：建议 Nginx 反向代理，无需后端 CorsConfig

## 默认账号

- 管理员：用户名 `admin` / 密码 `admin123`（角色 ADMIN，可访问审核工作台）
- 演示用户：用户名 `demo_user` / 密码 `demo123`（角色 USER）
