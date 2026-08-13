# 智观·古建 (ArchiVision Heritage)

> 以技术为舟，载文化远航 — 文化遗产数字化与智能创作平台

**智观·古建**是一个以古建筑文化遗产为核心的数字孪生与智能创作平台。平台集成了 VGGT 视觉引擎进行古建结构深度解析，支持 AI 3D 模型生成（一键幻筑），并构建了一个轻量、沉浸的古建文化社区。前端采用大厂级 UI/UX 设计规范与中国传统色彩体系，致力于让每一块瓦当、每一组斗栱在数字世界重获新生。

---

## 目录结构

```
ArchiVisionHeritage/
├── backend/                          # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/zhiguan/gujian/
│       │   ├── ZhiguanApplication.java      # 启动类
│       │   ├── config/                       # 配置 (CORS/MyBatis-Plus/Security/异步)
│       │   ├── controller/                   # 控制器
│       │   │   ├── AuthController.java       # 认证 (登录/注册)
│       │   │   ├── CommunityController.java  # 社区 (帖子/评论/点赞/关注/个人中心)
│       │   │   ├── HuanZhuController.java    # 一键幻筑 (AI 3D生成)
│       │   │   ├── ZhiXiController.java      # 古建智析 (VGGT结构解析)
│       │   │   ├── NotificationController.java  # 通知中心
│       │   │   └── AdminController.java      # 管理后台 (内容审核)
│       │   ├── model/                        # 数据实体
│       │   ├── mapper/                       # MyBatis-Plus Mapper
│       │   ├── service/                      # 业务逻辑层
│       │   ├── dto/                          # 请求/响应 DTO
│       │   ├── security/                     # JWT 工具 & 安全过滤器
│       │   ├── annotation/                   # 自定义注解 (@RateLimit)
│       │   ├── aop/aspect/                   # 切面 (限流)
│       │   ├── exception/                    # 全局异常处理
│       │   ├── constant/                     # 枚举常量
│       │   └── utils/                        # 工具类
│       └── resources/
│           ├── application.yml               # 数据库 & 应用配置
│           └── init.sql                      # 数据库初始化脚本
│
├── frontend/                         # Vue 3 前端
│   ├── vite.config.js                # Vite 配置 (含 /api 代理)
│   ├── index.html
│   └── src/
│       ├── main.js                    # 入口
│       ├── App.vue                    # 根组件 (导航栏 + 路由出口)
│       ├── router/index.js            # 路由配置 (含守卫)
│       ├── assets/style.css           # 全局样式 & Design Tokens
│       ├── stores/                    # Pinia 状态管理
│       │   ├── user.js                # 用户状态
│       │   └── notification.js        # 通知状态
│       ├── api/                       # Axios 接口封装
│       │   ├── request.js             # Axios 实例 (含拦截器)
│       │   ├── auth.js                # 认证接口
│       │   ├── community.js           # 社区接口
│       │   ├── huanzhu.js             # 幻筑接口
│       │   ├── zhixi.js               # 智析接口
│       │   ├── notification.js        # 通知接口
│       │   └── admin.js               # 管理接口
│       └── views/                     # 页面组件
│           ├── CommunityFeedView.vue   # 首页瀑布流
│           ├── PostDetailView.vue      # 帖子详情 & 3D展示
│           ├── LoginView.vue           # 登录/注册
│           ├── HuanZhuView.vue         # 一键幻筑 (AI 3D)
│           ├── ZhiXiView.vue           # 古建智析 (VGGT)
│           ├── NotificationListView.vue # 通知中心
│           ├── UserProfileView.vue     # 个人中心
│           ├── SettingsView.vue        # 设置
│           └── AdminDashboardView.vue  # 审核工作台
│
├── docs/
│   └── API接口文档.md                 # 完整 API 文档
├── prompt/
│   └── 古建平台前端UI优化方案.md       # UI 设计规范文档
└── 项目进程.md                        # 项目开发进程记录
```

---

## 核心功能

### 文化社区 (Community)
- **瀑布流首页**：基于 CSS Grid 的动态高度瀑布流布局，响应式适配多端
- **帖子发布**：支持关联 3D 模型资产，内容审核机制
- **社交互动**：点赞（含弹跳动画）、评论、关注
- **个人中心**：我的帖子、我的点赞、数字锦盒

### 古建智析 (ZhiXi — VGGT)
- **结构解析**：上传古建图片，VGGT 引擎推理三维结构（相机参数、点云、深度图）
- **文化解读**：AI 转译深奥的几何分析结果为通俗文化解读
- **限流保护**：单用户每日 5 次调用节制，429 响应 + 温润文化提示

### 一键幻筑 (HuanZhu — AI 3D)
- **文字生成 3D**：自然语言描述古建，AI 生成精美三维模型
- **智能提示**：按朝代/屋顶/色彩分类的文化要素快捷标签
- **异步任务**：提交后轮询状态，古塔构建动画填补等待焦虑
- **数字锦盒**：生成完成弹窗通知

### 管理后台 (Admin)
- **内容审核**：待审核帖子列表，审核通过/驳回
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
| 3D 渲染 | Google model-viewer (glTF/GLB) |
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
- 导入 VGGT 演示解析数据

### 2. 启动后端

```bash
cd backend
# 修改 src/main/resources/application.yml 中的数据库密码
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

开发环境下，Vite 自动将 `/api` 请求代理至 `http://localhost:8080`。

### 4. (可选) 启动 VGGT Python 服务

古建智析功能需要 Python VGGT API 服务在 `http://127.0.0.1:8000` 运行。如未启动，调用智析接口会返回 503 降级提示。演示数据功能不受影响。

---

## 设计规范

### 中国传统色彩体系 (Design Tokens) — V3 石色·琥珀·月季

| 令牌 | 语义 | Hex |
|------|------|-----|
| `--color-accent` | 琥珀金 | `#D97706` |
| `--color-accent-light` | 亮琥珀 | `#F59E0B` |
| `--color-rose` | 月季红 | `#E11D48` |
| `--color-bg-base` | 暖宣纸色 | `#F4F1EB` |
| `--color-surface` | 毛玻璃卡片 | `rgba(255,255,255,0.6)` + blur |
| `--color-text-main` | stone-800 | `#292524` |
| `--color-text-sub` | stone-500 | `#78716C` |
| `--color-border` | 极淡分割 | `rgba(0,0,0,0.06)` |

支持 `[data-theme="dark"]` 夜间模式自动切换。

### 4pt 间距系统

所有外边距、内边距、图标尺寸均为 4px 的整数倍：

| 变量 | 像素 | 用途 |
|------|------|------|
| `--spacing-xs` | 4px | 图标与文字间距 |
| `--spacing-sm` | 8px | 头像与昵称间距 |
| `--spacing-md` | 16px | 瀑布流卡片列间距 |
| `--spacing-lg` | 24px | 卡片内边距 |
| `--spacing-xl` | 32px | 模块间距 |
| `--spacing-xxl` | 64px | 导航栏高度 |

### 中文排版

跨平台字体栈（macOS 优先苹方/冬青黑体，Windows 回退微软雅黑）：
```
-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
"Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif
```

正文 14px / 行高 1.8，标题 24px / 字重 600。

---

## API 文档

完整 API 文档参见：[docs/API接口文档.md](docs/API接口文档.md)

### 接口概览

| 模块 | 端点 | 说明 |
|------|------|------|
| 认证 | `POST /api/v1/auth/login` | 登录 |
| 认证 | `POST /api/v1/auth/register` | 注册 |
| 社区 | `GET /api/v1/posts` | 瀑布流帖子列表 |
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

## 默认管理员账号

- 用户名：`admin`
- 密码：`admin123`
- 角色：ADMIN（可访问审核工作台）
