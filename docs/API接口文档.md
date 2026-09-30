# 智观·古建 API 接口文档

> 版本：v1.2 | 基础路径：`http://localhost:8080/api/v1` | 统一响应格式：`{ "code": 200, "message": "success", "data": {...} }`

---

## 1. 认证模块 (Auth)

### 1.1 用户注册
```
POST /api/v1/auth/register
Content-Type: application/json
```

**请求体：**
```json
{
  "username": "string (2-64字符，必填)",
  "password": "string (6+字符，必填)",
  "nickname": "string (必填)"
}
```

**响应：** `{ "code": 200, "message": "success", "data": null }`

### 1.2 用户登录
```
POST /api/v1/auth/login
Content-Type: application/json
```

**请求体：**
```json
{
  "username": "string (必填)",
  "password": "string (必填)"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": { "token": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

---

## 2. 社区模块 (Community)

> 需要认证的接口在请求头携带：`Authorization: Bearer <token>`

### 2.1 获取帖子流（瀑布流）
```
GET /api/v1/posts?page=1&size=12
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "postId": 1,
        "title": "山西应县木塔",
        "preview2dPath": "/assets/preview/xxx.jpg",
        "authorNickname": "古建爱好者",
        "likeCount": 42,
        "commentCount": 7,
        "likedByMe": false,
        "createdAt": "2026-05-14 10:30:00"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 12
  }
}
```

### 2.2 获取帖子详情
```
GET /api/v1/posts/{id}
Authorization: Bearer <token> (可选，用于判断当前用户是否已点赞)
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "postId": 1,
    "title": "山西应县木塔",
    "content": "应县木塔是中国现存最古老...",
    "preview2dPath": "/assets/preview/xxx.jpg",
    "glb3dPath": "/assets/models/xxx.glb",
    "authorNickname": "古建爱好者",
    "authorId": 1,
    "likeCount": 42,
    "commentCount": 7,
    "likedByMe": false,
    "followedByMe": false,
    "createdAt": "2026-05-14 10:30:00",
    "comments": [
      {
        "id": 1,
        "nickname": "访客",
        "content": "非常有价值的分享",
        "createdAt": "2026-05-14 11:00:00"
      }
    ]
  }
}
```

### 2.3 发布帖子
```
POST /api/v1/posts
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**
```json
{
  "title": "string (2-128字符，必填)",
  "content": "string (可选)",
  "modelAssetId": 1
}
```

**说明：** 帖子默认状态为 `PENDING`（待审核），管理员审核通过后状态变为 `APPROVED` 并公开展示。

### 2.4 发表评论
```
POST /api/v1/posts/{id}/comments
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**
```json
{
  "content": "string (必填，最长1024字符)",
  "parentId": null
}
```

### 2.5 点赞/取消点赞
```
POST /api/v1/interactions/like
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**
```json
{
  "targetId": 1,
  "targetType": "POST"
}
```
`targetType` 可选值：`POST` | `COMMENT`

### 2.6 关注/取消关注
```
POST /api/v1/users/{id}/follow
Authorization: Bearer <token>
```

---

## 3. 个人中心 (User Profile)

### 3.1 获取当前用户信息
```
GET /api/v1/users/me
Authorization: Bearer <token>
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatarUrl": "",
    "bio": "热爱古建筑文化",
    "role": "USER",
    "createdAt": "2026-05-14 10:00:00"
  }
}
```

### 3.2 更新个人资料
```
PUT /api/v1/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**
```json
{
  "nickname": "新昵称",
  "avatarUrl": "/assets/avatars/xxx.jpg",
  "bio": "新的文化签名"
}
```
所有字段均为可选，只更新传入的字段。

### 3.3 修改密码
```
PUT /api/v1/users/me/password
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**
```json
{
  "oldPassword": "当前密码",
  "newPassword": "新密码（至少6位）"
}
```

### 3.4 获取我的帖子
```
GET /api/v1/users/me/posts?page=1&size=12
Authorization: Bearer <token>
```

### 3.5 获取我的点赞
```
GET /api/v1/users/me/likes?page=1&size=12
Authorization: Bearer <token>
```

> 包结构说明：个人资料/密码接口（3.1-3.3）位于 **auth BC**（ProfileController + UserApplicationService）；我的帖子/点赞（3.4-3.5）位于 **community BC**（CommunityController → PostService/LikeService）。URL 契约不变。

---

## 4. 一键幻筑 (HuanZhu — AI 3D 生成)

### 4.1 提交幻筑任务
```
POST /api/v1/tasks/huanzhu
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**
```json
{
  "prompt": "唐代风格的重檐歇山顶大殿，配有绿琉璃瓦与朱红色的回廊"
}
```

**响应 (HTTP 202 Accepted)：**
```json
{
  "code": 200,
  "data": {
    "taskId": 42,
    "status": "PENDING"
  }
}
```

### 4.2 轮询任务状态
```
GET /api/v1/tasks/{id}/status
Authorization: Bearer <token>
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "taskId": 42,
    "status": "SUCCESS",
    "assetId": 10,
    "preview2dPath": "/assets/preview/xxx.png",
    "glb3dPath": "/assets/models/xxx.glb",
    "errorMessage": null
  }
}
```
`status` 枚举：`PENDING` → `RUNNING` → `SUCCESS` | `FAILED`

---

## 5. 古建智析 (ZhiXi — VGGT 结构解析)

### 5.1 VGGT 结构分析
```
POST /api/v1/analysis/zhixi
Content-Type: multipart/form-data
```

**请求参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| image | File | 古建图片 (JPG/PNG) |

**限流：** 单用户每日最多 5 次。超限返回 HTTP 429。

**成功响应：** 返回 VGGT 解析的 JSON 数据（转发自 Python VGGT API :8000）。

**Python 服务未就绪时响应：**
```json
{ "code": 503, "message": "VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)" }
```

### 5.2 获取演示数据列表（已废弃）

> 已废弃：前端已移除该调用（refactor/phase-5），保留仅为兼容旧客户端，后续版本删除。

```
GET /api/v1/analysis/zhixi/demos
```

### 5.3 古建智能修复接口（预留）
```
POST /api/v1/analysis/restoration/predict
```

**响应（业务码 501，HTTP 200 —— 占位接口直接返回 Result.fail）：**
```json
{ "code": 501, "message": "古建智能修复功能即将上线，敬请期待" }
```

---

## 6. 通知模块 (Notification)

### 6.1 获取通知列表
```
GET /api/v1/notifications
Authorization: Bearer <token>
```

### 6.2 获取未读通知数
```
GET /api/v1/notifications/unread-count
Authorization: Bearer <token>
```

**响应：**
```json
{ "code": 200, "data": { "count": 3 } }
```

### 6.3 标记单条已读
```
PUT /api/v1/notifications/{id}/read
Authorization: Bearer <token>
```

### 6.4 全部标记已读
```
PUT /api/v1/notifications/read-all
Authorization: Bearer <token>
```

---

## 7. 管理后台 (Admin)

> 需要 ADMIN 角色

### 7.1 获取待审核帖子
```
GET /api/v1/admin/posts/pending?page=1&size=20
Authorization: Bearer <token> (需要管理员权限)
```

### 7.2 审核帖子
```
PUT /api/v1/admin/posts/{id}/audit
Authorization: Bearer <token> (需要管理员权限)
Content-Type: application/json
```

**请求体：**
```json
{
  "status": "APPROVED",
  "rejectReason": ""
}
```
`status`：`APPROVED`（通过）或 `REJECTED`（驳回）。驳回时 `rejectReason` 必填。

---

## 8. 错误码说明

业务异常（`CulturalApiException`）的 **HTTP 状态码与 body 里的 `code` 始终一致**
（`GlobalExceptionHandler` 用 `HttpStatus.resolve(code)` 映射）。
唯一例外：占位接口直接 `return Result.fail(501, ...)`，不抛异常，故 HTTP 200 + body `501`。

| HTTP 状态码 | code | 说明 |
|------------|------|------|
| 200 | 200 | 请求成功 |
| 400 | 400 | 参数校验失败（字段级信息形如 `prompt: 描述不能超过 512 字`） |
| 401 | 401 | **未登录或 token 过期/无效**（Body 为统一结构；前端据此跳登录） |
| 403 | 403 | 已认证但无权限（如非管理员访问 `/api/v1/admin/**`） |
| 404 | 404 | 资源不存在（含：不存在的静态资源、未公开的内容按"不存在"处理） |
| 409 | 409 | 状态冲突（如审核非 `PENDING` 状态的帖子） |
| 413 | 413 | 上传文件超过 5MB |
| 429 | 429 | 请求频率超限（智析每日 5 次；登录每日 20 次） |
| 500 | 500 | 服务器内部错误 |
| 501 | 501 | 功能未实现（智能修复预留；**HTTP 200 + body code 501**） |
| 503 | 503 | VGGT 引擎未就绪（Python 服务未启动） |

> 已删除原表中的 `502 VGGT 分析服务暂时不可用`：`AnalysisService` 的 502 抛出位于 try 块内、
> 被 catch-all 吞掉，实际恒为 503 —— 原文档列了一个**永不返回**的错误码。

> `401` 与 `403` 的区分是**前端自动跳登录的前提**：`SecurityConfig` 显式配置了
> `RestAuthenticationEntryPoint`(401) 与 `RestAccessDeniedHandler`(403)，
> 两者都返回统一 JSON body。此前未配置时，Spring Security 6 默认把"未登录"也报成 403 且无 body。

## 9. 统一响应格式

所有接口统一返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

- `code`：业务状态码，200 表示成功（与 HTTP 状态码一致，见 §8）
- `message`：提示信息（**字段名是 `message`，不是 `msg`**）
- `data`：响应数据，可能为 null、对象或数组

## 10. 认证说明

- 登录成功后返回 JWT token，存储在客户端 localStorage
- 需要认证的接口在请求头携带：`Authorization: Bearer <token>`
- JWT payload 包含：`sub`(userId)、`username`、`role`(USER/ADMIN)
- Token 过期或无效时返回 HTTP 401

## 11. 文件上传模块 (Upload)

> 本模块此前**完全未文档化**，而前端 `frontend/src/api/upload.js` 一直在调用。

### 11.1 上传图片

**接口：** `POST /api/v1/upload/image`
**认证：** 需要登录
**Content-Type：** `multipart/form-data`

| 参数 | 位置 | 必填 | 说明 |
|---|---|---|---|
| `file` | form-data | 是 | 图片文件，≤ 5MB |
| `subDir` | form-data | 否 | 存放子目录，**白名单**：`covers` / `images` / `avatars`，默认 `images` |

**成功响应：**
```json
{ "code": 200, "message": "success", "data": { "url": "/assets/covers/<uuid>.jpg" } }
```

**错误：**

| 状态码 | 场景 |
|---|---|
| 400 | 文件为空 / 超过 5MB / `subDir` 不在白名单 / 文件内容不是可识别图片 |
| 401 | 未登录 |
| 413 | 超过 `spring.servlet.multipart` 的 5MB 上限（框架层先拦） |

**安全约束（实现要点，改动前请先读）：**
- 文件类型按**文件魔数**判定（JPEG / PNG / GIF / WebP），**不信任客户端声明的 `Content-Type`**
- 落盘扩展名**只由嗅探结果决定**，与原始文件名无关
  （否则把 HTML 命名为 `.html` 并声明 `image/png`，会在 `/assets/**` 下以 `text/html` 回读，构成同源存储型 XSS）
- `subDir` 必须匹配 `^[A-Za-z0-9_-]{1,32}$`，落盘后再断言目标仍在 assets 根内（双层防护）
- 返回的 URL 前缀来自配置 `zhiguan.assets.url-prefix`，不是硬编码 `/assets`

---

## 修订说明（2026-09-30 文档校正）

本次对文档做了逐条与实现比对的校正，涉及：

| 位置 | 原描述 | 更正为 |
|---|---|---|
| 全文 | 无上传模块 | 新增 §11（含安全约束） |
| §4.2 | `modelAssetId` | `assetId`（`TaskStatusResponse` 的真实字段名） |
| §2.1 | 响应含 `likedByMe`（当时实现里没有） | **实现已补齐该字段**（`PostBriefResponse`），文档与实现一致 |
| §2.1 / §2.2 | 未列 `modelAssetId` / `rejectReason` / `status` | 实现已补齐，文档同步 |
| §7.2 | "驳回时 `rejectReason` 必填"（当时后端不校验） | **后端已强制校验**（空理由 → 400），并补状态机：仅 `PENDING` 可审核，非法转移 → 409 |
| §8 | `502 VGGT 服务不可用` | 删除（实际不可达，恒 503） |
| §8 | `501` 为 HTTP 501 | 更正为 **HTTP 200 + body code 501** |
| §8 | 无 409 / 413 | 补充 |
| §9 | 字段名写作 `msg` | 更正为 `message` |
| §10 | "Token 过期返回 401"（当时实际 403 且无 body） | **实现已修复**：401 + 统一 body |
| §2.3 | 未写 `preview2dPath` / `tags` | 补充；并说明**管理员发帖直接 `APPROVED`**（普通用户为 `PENDING`） |
| §2.3 | `title` 为 "2-128 字符" | 实现为 `@NotBlank @Size(max=128)`，**无下限** |
| §6 | 无响应体说明 | 补充 `NotificationResponse` 结构，并提示 Lombok 会把 `boolean isRead` 序列化为 **`read`** |
| §3.2 / §3.3 | 描述为 JSON body 字段 | 实现接收 `Map<String,String>`，无 DTO、无 `@Valid`（未知字段静默忽略） |

> 已实现侧的行动详见 `项目进程.md`「第十一批次」与 `.workbuddy/_backend-fix-plan.md`。

