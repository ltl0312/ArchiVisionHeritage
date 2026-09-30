# 智观·古建 V4 UI 重构 — 视图实现共享规约（子代理必读）

本文件是本次 UI 重构中**所有视图实现任务的共同约束**。任何与本文件冲突的「个人审美」都不成立；
若你认为某条约束做不到，**停下来报告**，不要自行绕过。

---

## 0. 项目与工具链

- 仓库根：`D:\Code\JavaWeb\ArchiVisionHeritage`
- 前端根：`D:\Code\JavaWeb\ArchiVisionHeritage\frontend`
- 技术栈：Vue 3 `<script setup>` + Vite 5 + Element Plus（`unplugin-vue-components` 按需自动引入，
  模板里直接用 `<el-xxx>` 即可，**不需要手写 import**；图标必须从 `@element-plus/icons-vue` 显式 import）
- 构建校验（必须通过）：
  ```
  cd D:\Code\JavaWeb\ArchiVisionHeritage\frontend
  node node_modules/vite/bin/vite.js build
  ```

## 1. 先读这三个文件（顺序不能变）

1. `frontend/src/assets/style.css`
   —— **设计系统唯一来源**：全部令牌（`--color-*` / `--spacing-*` / `--radius-*` / `--shadow-*` /
   `--font-size-*` / `--transition-*`）与全部 V4 组件原语（`.v4-*`）都在这里。
   你的视图**只能用这里已有的类与令牌**；缺什么就报告，不要自己造一套。
2. `frontend/preview/v4-preview.html`
   —— **视觉基准**（7 个页面全部已验证）。按你任务里给出的行区间阅读对应页面与对应 CSS 块。
   注意：预览页用的是它自己的一套短变量名（`--gold` / `--tx-1` / `--surface` …），
   实现时必须**翻译成 style.css 的长令牌名**（`--color-accent` / `--color-text-main` / `--color-surface` …）。
3. 你自己的目标视图文件（重构类任务）——先读懂它现在的数据流与交互，**功能不能回退**。

## 2. 硬性约束（会被 grep 与构建检查）

### 2.1 令牌完整度：视图内不得出现字面色值
在 `frontend/src/**`（`style.css` 除外）中：
- 不允许出现 `#` 开头的十六进制色值
- 不允许出现 `rgba(` / `rgb(` / `hsl(`

需要「图片上的半透明遮罩 / 影像上的文字色」时，用已定义好的媒体令牌：
`--color-scrim-strong` `--color-scrim-mid` `--color-scrim-soft` `--color-media-line`
`--color-media-line-strong` `--color-media-fill` `--color-on-media` `--color-on-media-strong`
`--color-on-media-dim` `--color-on-media-faint`
或直接用原语类 `.v4-scrim` / `.v4-scrim--flat` / `.v4-scrim--strong`。

自检命令（把你自己的文件路径填进去，输出必须为空）：
```
cd D:\Code\JavaWeb\ArchiVisionHeritage\frontend
Select-String -Path src\views\你的文件.vue -Pattern '#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\('
```

### 2.2 视图根节点
每个视图的**模板根元素**必须是：
```html
<div class="view-shell"> ... </div>
```
沉浸式页面（头图要顶到顶栏下沿）用 `class="view-shell view-shell--flush"`，
然后在内部自己用 `.v4-immers` 结构（见预览页 `#v-archive`）。

`.view-shell` 提供统一的 `padding: var(--spacing-xl)`，并且是**信息密度偏好（紧凑模式）的唯一作用点**。
视图宿主 `.view-host` 已经是滚动容器（`App.vue` 里），**视图自己不要再套一层 `overflow-y:auto` 或 `height:100%`**。

### 2.3 四态齐全
每个**数据界面**都必须显式实现四种状态，不能只写「有数据」那条分支：
- **加载中**：`el-skeleton` 或骨架块 / 旋转图标（用 `class="loading-spin"`）
- **空**：`<div class="v4-empty">…</div>`，文案要具体（例：「还没有档案 · 去一键幻筑创建第一件」）
- **错误**：明确说明失败原因与重试动作（`useAsyncAction` 默认吞掉错误，需要错误态就自己 `try/catch`）
- **无权限**：需要登录/管理员时给出说明与入口，而不是白屏

### 2.4 移动端
- 主布局一律用 `.v4-bench--3` / `.v4-bench--2` / `.v4-settings-grid` / `.v4-admin-grid` /
  `.v4-immers-body` / `.v4-vwrap` / `.v4-caps` / `.v4-cards3` 这些**已有栅格类**，
  全局 `@media (max-width: 900px)` 区块已经把它们降级为单列 —— **不要再重复写一遍**。
- 视图内允许写 `@media (max-width: 900px)` 做**本视图专属**的微调，但不允许再写 `768px` 断点
  （项目已统一到 900px 单一来源）。
- ≤390px 不得出现横向溢出。

### 2.5 可访问性
- 可点元素用 `<button>` / `<router-link>`，不要用 `<div @click>`。
- 开关：`role="switch"` + `:aria-checked="..."` + `aria-label`。
- 弹出菜单：`aria-haspopup="menu"` + `:aria-expanded` + `aria-controls`，点外部与 `Esc` 可关。
- 纯图标按钮必须有 `aria-label`。

### 2.6 不要碰的东西
- `src/api/**`、`src/stores/**`、`src/composables/**`、`src/utils/**`、`src/router/index.js`、
  `src/App.vue`、`src/assets/style.css`、`src/components/Layout/**`
  —— 这些已由主协调者改好。**你只改你自己的目标文件**（新建任务则新建该文件）。
- 后端（`backend/**`）：**一行都不许改**（计划书决策 3）。
- 不要新增 npm 依赖。

## 3. 可用的共享件（已实现，直接用）

| 组件 / 类 | 用途 |
|---|---|
| `@/components/ArchiveCard.vue` | 档案卡：`<ArchiveCard :to="..." :title :image :meta :tags :badge>`；无 `to` 时 emit `select` |
| `@/components/Layout/TopBar.vue` | 顶栏（已在 App.vue 挂载，视图不要重复实现） |
| `@/components/Layout/UserMenu.vue` | 账户菜单（顶栏与侧栏共用） |
| `@/components/CommentList.vue` | 评论区（PostDetailView 使用） |
| `@/components/ImageUploader.vue` | 上传组件（支持上传前压缩） |
| `.view-shell` | 视图外壳（统一页边距 + 密度作用点） |
| `.v4-card` `.v4-cardhd` `.v4-sechead` `.v4-toolrow` `.v4-headstats` | 卡片与分区结构 |
| `.v4-pill`（`--gold` `--jade` `--red` `--mut` `--bare`） | 状态胶囊（三色语义） |
| `.v4-btn`（`--gold` `--ghost` `--outline` `--block` `--lg` `--sm`） | 按钮 |
| `.v4-chip` / `.v4-chips` 容器 / `.v4-seg` | 标签与分段控件 |
| `.v4-switch` + `.is-on` | 开关 |
| `.v4-empty` / `.v4-tip` / `.v4-rules` / `.v4-loglist`+`.v4-logrow` | 空态 / 提示块 / 规则清单 / 操作日志 |
| `.v4-bench` `.v4-col` `.v4-zone` | 工作台栅格 |
| `.v4-drop` `.v4-fieldset` `.v4-opt` | 上传投放区 / 选项组 |
| `.v4-viewport` `.v4-vp-scrim` `.v4-vp-top` `.v4-vp-meta` `.v4-corner--tl/tr/bl/br` `.v4-hot` `.v4-dock` | 三维视口 |
| `.v4-craft` `.v4-step`（`.is-done` `.is-now`） | 工序时间轴 |
| `.v4-compose` `.v4-textarea` | 创作输入 |
| `.v4-hero*` `.v4-caps` `.v4-cap` `.v4-statpanel` `.v4-stat` `.v4-cards3` `.v4-arc` | 营造志首屏 |
| `.v4-immers*` `.v4-floatbar` `.v4-backpill` `.v4-vwrap` `.v4-viewer` `.v4-prose` `.v4-dyn` | 沉浸详情 |
| `.v4-kv`（`.k`/`.v`/`.v--jade`/`.v--gold`/`.v--mut`） `.v4-kv-row` `.v4-switch-row` `.v4-note` | 键值行 / 设置行 |
| `.v4-audit-row` `.v4-audit-list` `.v4-admin-grid` | 审核工作台 |
| `.v4-notif-row` `.v4-nicon--gold/jade/red` `.v4-nbody` `.v4-nact` `.v4-unread-dot` | 通知中心 |
| `.v4-settings-grid` | 设置页双列 |

## 4. 后端接口事实（不可改，只能按它实现）

| 接口 | 返回 |
|---|---|
| `GET /api/v1/posts?page&size` | `Page<PostBriefResponse>`：`postId,title,preview2dPath,authorNickname,authorAvatarUrl,likeCount,commentCount,status,tags,createdAt` |
| `GET /api/v1/posts/{id}` | `PostDetailResponse`：+`content,glb3dPath,authorId,likedByMe,followedByMe,comments[]` |
| `POST /api/v1/posts` | 发帖（默认 `PENDING` 待审） |
| `POST /api/v1/posts/{id}/comments` | 评论 |
| `POST /api/v1/interactions/like` | `{targetId, targetType:'POST'}` |
| `POST /api/v1/users/{id}/follow` | 关注 |
| `GET /api/v1/users/me/posts?page&size` | 我发布的帖子（`Page<PostBriefResponse>`） |
| `GET /api/v1/users/me/likes?page&size` | 我点赞过的帖子 |
| `GET /api/v1/users/me` | `{id,username,nickname,avatarUrl,bio,role,createdAt}` |
| `PUT /api/v1/users/me` | `{nickname?,avatarUrl?,bio?}` |
| `PUT /api/v1/users/me/password` | `{oldPassword,newPassword}`（新密码 ≥6 位） |
| `GET /api/v1/notifications` | `[{id,taskId,message,isRead,createdAt}]` —— **没有 type 字段** |
| `GET /api/v1/notifications/unread-count` | `{count}` |
| `PUT /api/v1/notifications/{id}/read`、`PUT /api/v1/notifications/read-all` | 已读 |
| `GET /api/v1/admin/posts/pending?page&size` | `Page<PostBriefResponse>`（`status=PENDING`） |
| `PUT /api/v1/admin/posts/{id}/audit` | `{status:'APPROVED'\|'REJECTED', rejectReason?}` |
| `POST /api/v1/analysis/zhixi` | multipart `file` → 解析结果（见 `ZhiXiView` 现有实现） |
| `GET /api/v1/analysis/zhixi/demos` | 演示解析数据 |
| `POST /api/v1/tasks/huanzhu` | `{prompt}` → `{taskId}`（HTTP 202） |
| `GET /api/v1/tasks/{id}/status` | `{taskId,status,assetId,preview2dPath,glb3dPath,errorMessage}`；`status ∈ PENDING/RUNNING/SUCCESS/FAILED` |
| `POST /api/v1/upload/image` | `{url}` |

**统一响应**：`{ code: 200, message, data }`，`src/api/request.js` 的拦截器已把 `data` 拆出来
（即 `await api.x()` 拿到的是 `res.data`）。非 200 会自动 `ElMessage.error`。

### 重要：不要伪造数据
后端没有的字段（如通知分类、额度余量、审核历史统计、帖子朝代/置信度）**一律不要编造数字**。
要么从已有字段**可推导地**得到，要么不展示，要么明确标注来源（例：「本次会话操作」）。
设计稿里有但后端无数据源的元素，**省略比造假更好**，并在你的交付说明里写清楚省略了什么、为什么。

## 5. 交付说明（你必须在最终回复里给出）

1. 你改了/新建了哪些文件（绝对或仓库相对路径）
2. 数据流：每个接口调用点 + 四态各自如何呈现
3. `node node_modules/vite/bin/vite.js build` 的实际结果
4. 你的文件的 `#hex / rgba(` 自检结果（必须为空）
5. 你**故意省略或偏离设计稿**的地方及理由
6. 你没能做到 / 不确定的地方
