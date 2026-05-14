# 智观·古建 API 接口文档

> 版本：v1.2 | 基础路径：`http://localhost:8080/api/v1` | 统一响应格式：`{ "code": 200, "msg": "success", "data": {...} }`

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

**响应：** `{ "code": 200, "msg": "success", "data": null }`

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
  "msg": "success",
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
    "modelAssetId": 10,
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
{ "code": 503, "msg": "VGGT 深度解析引擎未就绪，请确认 Python 服务已启动 (端口 8000)" }
```

### 5.2 获取演示数据列表
```
GET /api/v1/analysis/zhixi/demos
```

### 5.3 古建智能修复接口（预留）
```
POST /api/v1/analysis/restoration/predict
```

**响应 (HTTP 501)：**
```json
{ "code": 501, "msg": "古建智能修复功能即将上线，敬请期待" }
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

| HTTP 状态码 | code | 说明 |
|------------|------|------|
| 200 | 200 | 请求成功 |
| 400 | 400 | 参数校验失败 |
| 401 | 401 | 未登录或 token 过期 |
| 403 | 403 | 无权限（如非管理员访问管理接口） |
| 404 | 404 | 资源不存在 |
| 429 | 429 | 请求频率超限（VGGT 每日5次限制） |
| 500 | 500 | 服务器内部错误 |
| 501 | 501 | 功能未实现（智能修复预留） |
| 502 | 502 | VGGT 分析服务暂时不可用 |
| 503 | 503 | VGGT 引擎未就绪 |

## 9. 统一响应格式

所有接口统一返回以下 JSON 结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

- `code`：业务状态码，200 表示成功
- `msg`：提示信息
- `data`：响应数据，可能为 null、对象或数组

## 10. 认证说明

- 登录成功后返回 JWT token，存储在客户端 localStorage
- 需要认证的接口在请求头携带：`Authorization: Bearer <token>`
- JWT payload 包含：`sub`(userId)、`username`、`role`(USER/ADMIN)
- Token 过期或无效时返回 HTTP 401
