# 后端修复计划与压缩上下文（断点续接用）

> 用途：把「前端 V4 重构 + 后端审计」的结论压缩成可续接的状态，并作为后端修复的执行计划。
> 生成时间：2026-09-30 · 基线 HEAD `2f9f827` · 分支 `main`（已与 origin 同步）

---

## 0. 压缩上下文：项目当前状态

### 已完成并验证
| 阶段 | 内容 | 证据 |
|---|---|---|
| 前端 V4 重构 | 计划书 P0–P4 全部完成，13 个提交已推送 | CI `2f9f827` **绿色**；`.workbuddy/shots/report.json`（36 样本全绿） |
| 前端 CI 修复 | `.gitignore` 裸 `api/` 吞掉 `src/api/upload.js` 导致全新克隆无法构建 | 容器内 `git clone` + `npm ci` + `build` 成功 |
| Docker 集成测试 | 3 镜像构建 + 全链路 E2E **35/35** | `.workbuddy/_e2e_docker.mjs` |
| 后端审计 | 3 安全漏洞 + 7 功能缺陷 + 11 契约缺口 + 测试缺口 + 文档漂移 | 见 §1，均有 PoC 或 `文件:行号` |

### 运行环境（修复验证依赖它）
```bash
# 栈仍在运行（前端 http://localhost:8088）
docker compose -f docker-compose.yml -f docker-compose.verify.yml ps
# 重建后端并重启（每次改完后端代码后执行）
docker compose -f docker-compose.yml -f docker-compose.verify.yml build backend
docker compose -f docker-compose.yml -f docker-compose.verify.yml up -d backend
# 本地单元测试（Java 17 + Maven 3.9.12 已装）
cd backend && mvn -q test
```
> `docker-compose.verify.yml` 的存在原因：本机 8080 被无关容器 `searxng` 占用、3306 被本机 MySQL 占用，
> 故用 `!reset`/`!override` 重置发布端口，只把前端发布到 8088，其余走容器网络（真实生产链路）。

### 关键约束
- **决策 3 已由用户解除**：用户明确「自行开始修复计划」→ 允许改后端。
- 每批次独立提交（中文信息），每批次后跑 `mvn test` + 重建镜像 + 跑 PoC/E2E 验证。
- 不得为了让测试变绿而放宽断言；不得用「文档改一改」掩盖实现缺陷。
- 前端已按「不伪造数据」退让之处（§1.C），修复后**不要求**回改前端，但要在文档里记明可用性提升。

---

## 1. 待修清单（按严重度，含精确位置与验证方式）

### A. 安全（最高优先）
| ID | 问题 | 位置 | 修法 | 验证 |
|---|---|---|---|---|
| A1 | JWT 默认密钥可伪造 ADMIN | `application.yml:56`；`docker-compose.yml` 未注入 `JWT_SECRET` | 主配置去掉默认值改为必填；compose 注入；`.env.example` 说明 | 重跑 `_poc_jwt_default_secret.mjs` → ①应被拒 |
| A2 | 上传存储型 XSS | `UploadController.java:49-59` 只信客户端 Content-Type；`FileUtil.java:34-35` 扩展名取自文件名 | 按**魔数**嗅探真实类型；扩展名由嗅探结果决定 | 重跑 `_poc_upload_xss.mjs` → A 段应被拒 |
| A3 | 上传路径穿越 | `FileUtil.java:30` 直接拼 `subDir` | `subDir` 白名单校验（拒绝 `..`/分隔符） | `_poc_upload_xss.mjs` B 段 → 应被拒，`/traversal-poc` 不再出现 |
| A4 | IDOR：任务状态无归属校验 | `HuanZhuController.java:48-55` | Service 加 userId 参数并校验归属 | 新 E2E：用户 A 查 B 的 taskId → 403/404 |
| A5 | CORS 通配 + 携带凭据 | `CorsConfig.java:15-18` | 改为可配置的 origin 白名单 | 配置审查 + 预检请求实测 |
| A6 | 登录无限流 | `AuthController.java:21` | 加 `@RateLimit`（按 IP/用户名） | 连续错误登录 → 429 |

### B. 内容治理
| ID | 问题 | 位置 | 修法 | 验证 |
|---|---|---|---|---|
| B1 | 未审核/已驳回内容可匿名读取 | `PostServiceImpl.java:57-59` 不按 status 过滤；`SecurityConfig.java:45` permitAll | 详情加可见性规则：APPROVED 公开；否则仅作者/管理员 | `_poc_pending_visibility.mjs` → ②③应被拒，④仍可见 |
| B2 | 审核状态机未实现 | `PostServiceImpl.java:137-148` | 强制 PENDING → APPROVED/REJECTED；service 内也白名单校验 status | `_poc_audit_likes.mjs` ① → 非法转移应全部被拒 |

### C. 接口契约缺口（前端已退让处）
| ID | 缺什么 | 修法 | 前端可用性提升 |
|---|---|---|---|
| C1 | `PostBriefResponse` 无 `likedByMe` | 装配器接收 currentUserId 并计算 | **修复信息流点赞态反转**（F3） |
| C2 | `PostBriefResponse`/`PostDetailResponse` 无 `rejectReason` | 两处 DTO 加字段并赋值 | 作者能看到驳回原因 |
| C3 | `PostBriefResponse` 无 `modelAssetId` | DTO 加字段 | 「我的档案」可区分解析/幻筑 |
| C4 | `NotificationResponse` 无 `type` | `notification` 表加列 + 生产/响应/DTO | 通知可分类 |
| C5 | `TaskStatusResponse.assetId` 与文档不符 | 改文档（前端已按 `assetId` 对接） | 无代码改动 |

### D. 健壮性与一致性
| ID | 问题 | 位置 | 修法 |
|---|---|---|---|
| D1 | `NoResourceFoundException` 兜底成 500 | `GlobalExceptionHandler.java:57-62` | 显式处理 → 404 |
| D2 | 401/403 无统一 body，且过期 token 返回 403 | `SecurityConfig` 未配 EntryPoint/DeniedHandler | 配 `AuthenticationEntryPoint`(401) + `AccessDeniedHandler`(403)，输出 `{code,message,data}` |
| D3 | `HuanZhuRequest.prompt` 无长度上限 | `HuanZhuRequest.java:8-9` | `@Size(max=512)` 对齐 `original_prompt VARCHAR(512)` |
| D4 | `LikeRequest.targetType` 无白名单 | `LikeRequest.java:12-13` | `@Pattern(POST|COMMENT)` |
| D5 | 给不存在的帖子点赞/评论 | `LikeServiceImpl`/`CommentServiceImpl` | 校验目标存在 |
| D6 | `comment.parentId` 存了不用 | `CommentServiceImpl.getComments` | 按 parentId 组装两级嵌套 |
| D7 | `register` 并发竞态 → 500 | `AuthServiceImpl.java:39-49` | 捕获 `DuplicateKeyException` → 400 |
| D8 | `AsyncConfig` 无拒绝策略/优雅停机 | `AsyncConfig.java:21-27` | `CallerRunsPolicy` + `setWaitForTasksToCompleteOnShutdown` |
| D9 | docker profile 不叠加 prod → SQL 日志 + DEBUG 上生产 | `application-docker.yml` | 引入 prod 的日志治理 |
| D10 | `FileUtil` 硬编码 `/assets/` 前缀 | `FileUtil.java:42` | 用 `zhiguan.assets.url-prefix` |
| D11 | 幻筑资产从不落盘 → 幻筑结果必然破图 | `TaskExecutionService.java:76-80` | 生成真实占位封面 PNG + GLB 占位文件（写盘） |
| D12 | 通知列表无分页 | `NotificationServiceImpl.java:26-39` | 加分页（保留旧签名的兼容重载） |

### E. 测试
| ID | 内容 |
|---|---|
| E1 | 为 5 个无测试 Controller 补 `@SpringBootTest + @AutoConfigureMockMvc` 端到端（含 Security 链） |
| E2 | 修 8 处假测试（`NotificationServiceImplTest:126-139/63-75`、`JwtUtilTest:89-95`、`PostServiceTest:90-108`、`TaskExecutionServiceTest:98-107`、`LikeServiceTest/FollowServiceTest/TaskOrchestrationServiceImplTest` 的全通配符断言、`ZhiXiControllerTest:84-92`） |
| E3 | `schema-h2.sql`：ENUM → `CHECK (status IN (...))`；补种子数据（admin/demo_user/analysis_demo） |
| E4 | 分页方言按 profile 切换（H2 profile 用 `DbType.H2`） |
| E5 | 为本次修复的每个缺陷补回归测试（A1–A3、B1–B2、C1–C3、D1–D3、D5、D7） |

### F. 文档
| ID | 内容 |
|---|---|
| F1 | `docs/API接口文档.md`：补上传接口、修 `likedByMe`/`modelAssetId`→`assetId`/401·403/501/502/rejectReason 必填/title min 等全部漂移 |
| F2 | `README.md` 接口概览补全至 26 个端点 |
| F3 | `CLAUDE.md`：VGGT 透传说明、upload 需登录、审计结论摘要 |

---

## 2. 执行顺序与检查点

| 批次 | 内容 | 状态 | 验证 |
|---|---|---|---|
| **B1** | A1 A2 A3（安全三件套） | ✅ 完成 `8176c89` | 3 个 PoC 转绿；`mvn test` 135 全绿；E2E 35/35 |
| **B2** | B1 B2（内容治理） | ✅ 完成 `d1a5810` | 2 个 PoC 转绿；`mvn test` 147 全绿；E2E 35/35 |
| B3 | C1 C2 C3 C5（契约） | ⏳ 进行中 | 字段实测 + E2E |
| B4 | D1 D2 D3 D4 D7 D8 D9 D10（健壮性） | 待办 | 401/403 body 实测 |
| B5 | D5 D6 D11 D12（功能补完） | 待办 | E2E |
| B6 | E1–E5（测试） | 待办 | `mvn test` 全绿 + 新增测试有效 |
| B7 | F1–F3（文档） | 待办 | 逐条比对 |

### 本地验证环境（重要）
- 独立 Redis 供 `mvn test` 使用：`docker run -d --name zhiguan-redis-test -p 6379:6379 redis:7-alpine`
  （`docker-compose.verify.yml` 把 6379 的发布重置了，所以集成测试需要它）
- 改完后端代码后的验证循环：
  ```bash
  cd backend && mvn -B test
  docker compose -f docker-compose.yml -f docker-compose.verify.yml build backend
  docker compose -f docker-compose.yml -f docker-compose.verify.yml up -d backend
  # 等 healthy 后跑 PoC / E2E
  ```
- `JWT_SECRET` 现在必填（本地 `.env` 已有随机值）。**改密钥后旧 token 全部失效**，PoC 每次自行登录。

### 执行中发现并附带修掉的缺陷（不在原清单）
| 发现 | 处理 |
|---|---|
| `Set.of(...).contains(null)` 抛 NPE → `status=null` 变 500 | B2 已修（先判 null 再查白名单） |
| `MockMvc` 空字符串参数被视为"未提供" → 回落默认目录 | 已单列一个测试说明这是安全行为 |
| 审核状态机修复后，原 PoC「已发布帖被驳回后仍在收藏」场景**不再可构造** | 改写 PoC 为「待审帖被点赞后仍在收藏」 |

**每批次完成动作**：`mvn -q test` → 重建 backend 镜像 → 重启 → 跑相关 PoC/E2E → 中文提交。

---

## 3. 续接提示（Resume Hint）

- 先读本文件 §2 的批次状态表，找到第一个非「完成」的批次。
- 再跑一次 §1 对应行的「验证」命令确认现状（不要凭记忆）。
- 若 `docker compose ps` 显示栈已停：`docker compose -f docker-compose.yml -f docker-compose.verify.yml up -d`。
- 所有 PoC 脚本在 `.workbuddy/_poc_*.mjs`，用法统一：`node <脚本> http://127.0.0.1:8088`。
